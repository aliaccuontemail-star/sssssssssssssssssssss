package ir.pinbom.app.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.pinbom.app.R
import ir.pinbom.app.data.PinBomRepository
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.data.remote.Comment
import ir.pinbom.app.data.remote.Notification
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.data.remote.SearchBoard
import ir.pinbom.app.data.remote.SearchUser
import ir.pinbom.app.data.remote.UserProfile
import ir.pinbom.app.work.ImagePreparer
import ir.pinbom.app.work.UploadEvents
import ir.pinbom.app.work.UploadKeys
import ir.pinbom.app.work.UploadStage
import ir.pinbom.app.work.UploadWorker
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class FeedSource(
    private val repo: PinBomRepository,
    private val kind: String
) : PagingSource<String, Pin>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Pin> = try {
        val page = repo.feed(kind, params.key.orEmpty())
        repo.cache(page)
        LoadResult.Page(
            data = page.items,
            prevKey = null,
            nextKey = page.next_cursor.ifBlank { null }
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<String, Pin>): String? = null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    private val kind = MutableStateFlow("home")

    /** Additive UI state: the feed chips read which feed is active. */
    val selectedKind: StateFlow<String> = kind.asStateFlow()
    val items = kind
        .flatMapLatest { selected ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                FeedSource(repo, selected)
            }.flow
        }
        .cachedIn(viewModelScope)
    val cached = repo.cachedPins()

    fun setKind(value: String) {
        kind.value = value
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val query = MutableStateFlow("")
    val type = MutableStateFlow("pins")

    val results: Flow<PagingData<Pin>> = combine(query, type) { q, selectedType -> q to selectedType }
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { (q, selectedType) ->
            if (q.isBlank() || selectedType != "pins") {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                    object : PagingSource<String, Pin>() {
                        override suspend fun load(params: LoadParams<String>): LoadResult<String, Pin> = try {
                            val page = repo.search(q, params.key.orEmpty())
                            LoadResult.Page(
                                data = page.items,
                                prevKey = null,
                                nextKey = page.next_cursor.ifBlank { null }
                            )
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }

                        override fun getRefreshKey(state: PagingState<String, Pin>): String? = null
                    }
                }.flow
            }
        }
        .cachedIn(viewModelScope)

    val users = MutableStateFlow<List<SearchUser>>(emptyList())
    val boards = MutableStateFlow<List<SearchBoard>>(emptyList())

    /** Additive UI state: boards/people lists show a skeleton while loading. */
    val secondaryBusy = MutableStateFlow(false)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setType(value: String) {
        type.value = value
        searchSecondary(query.value, value)
    }

    fun searchSecondary(q: String, selectedType: String) {
        viewModelScope.launch {
            if (q.isBlank()) {
                users.value = emptyList()
                boards.value = emptyList()
                return@launch
            }
            secondaryBusy.value = true
            if (selectedType == "users") {
                runCatching { repo.searchUsers(q) }
                    .onSuccess { users.value = it.items }
            } else if (selectedType == "boards") {
                runCatching { repo.searchBoards(q) }
                    .onSuccess { boards.value = it.items }
            }
            secondaryBusy.value = false
        }
    }
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val pin = MutableStateFlow<Pin?>(null)
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val related = MutableStateFlow<List<Pin>>(emptyList())

    /* Phase 6: additive UI state only — drives the detail skeleton / error view. */
    val loading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    fun load(id: Long) {
        viewModelScope.launch {
            loading.value = true
            failed.value = false
            runCatching { repo.pin(id) }
                .onSuccess { pin.value = it }
                .onFailure { failed.value = true }
            runCatching { repo.comments(id) }.onSuccess { comments.value = it.items }
            runCatching { repo.related(id) }.onSuccess { related.value = it.items }
            loading.value = false
        }
    }

    fun like() {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            val result = if (pin.value?.liked == true) repo.unlike(id) else repo.like(id)
            val old = pin.value ?: return@launch
            val newLiked = result.liked ?: !old.liked
            pin.value = old.copy(
                liked = newLiked,
                likes = (old.likes + if (newLiked && !old.liked) 1 else if (!newLiked && old.liked) -1 else 0).coerceAtLeast(0)
            )
        }
    }

    fun save(board: Long? = null) {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            repo.save(id, board)
            pin.value = pin.value?.copy(saved = true)
        }
    }

    fun unsave() {
        val id = pin.value?.id ?: return
        viewModelScope.launch {
            repo.unsave(id)
            pin.value = pin.value?.copy(saved = false)
        }
    }

    fun comment(text: String) {
        val id = pin.value?.id ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.comment(id, text)
            runCatching { repo.comments(id) }.onSuccess { comments.value = it.items }
        }
    }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val busy = MutableStateFlow(false)

    /** Server-provided text, shown exactly as it arrives. */
    val error = MutableStateFlow("")

    /**
     * True when a failure carried no server text of its own. The screen words
     * those itself, so they arrive in the app's current language.
     */
    val failed = MutableStateFlow(false)

    fun clearFeedback() {
        error.value = ""
        failed.value = false
    }

    fun login(identifier: String, password: String, done: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = ""
            failed.value = false
            runCatching { repo.login(identifier, password) }
                .onSuccess { done() }
                .onFailure {
                    // HTTP error bodies are API envelopes, not user-facing prose.
                    // Let AuthScreen show its localized “check username/password”
                    // message instead of the unhelpful Retrofit text “HTTP 401”.
                    error.value = if (it is HttpException) "" else it.message.orEmpty()
                    failed.value = it is HttpException || it.message.isNullOrBlank()
                }
            busy.value = false
        }
    }

    fun register(username: String, email: String, password: String, done: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = ""
            failed.value = false
            runCatching { repo.register(username, email, password) }
                .onSuccess { done() }
                .onFailure {
                    // The registration fallback is localized and specifically
                    // suggests trying another username or email.
                    error.value = if (it is HttpException) "" else it.message.orEmpty()
                    failed.value = it is HttpException || it.message.isNullOrBlank()
                }
            busy.value = false
        }
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: PinBomRepository
) : ViewModel() {
    val user = MutableStateFlow<UserProfile?>(null)
    val boards = MutableStateFlow<List<Board>>(emptyList())
    val pins = MutableStateFlow<List<Pin>>(emptyList())
    val saved = MutableStateFlow<List<Pin>>(emptyList())

    /* Phase 7: additive UI state only — drives the profile skeleton / retry. */
    val loading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)
    val mediaBusy = MutableStateFlow(false)
    val mediaMessage = MutableStateFlow("")
    val mediaAction = MutableStateFlow("")

    fun uploadProfileMedia(uri: Uri, kind: String, target: String) {
        if (mediaBusy.value || (kind != "avatar" && kind != "cover")) return
        viewModelScope.launch {
            mediaBusy.value = true
            mediaMessage.value = ""
            var prepared: File? = null
            runCatching {
                prepared = withContext(Dispatchers.IO) {
                    ImagePreparer.prepare(
                        context = context,
                        uri = uri,
                        dir = File(context.cacheDir, "profile-media"),
                        name = ImagePreparer.fileNameFor("profile-${kind}-${UUID.randomUUID()}"),
                    )
                }
                repo.uploadProfileMedia(checkNotNull(prepared), kind)
            }.onSuccess {
                mediaAction.value = "saved:${System.nanoTime()}"
                load(target)
            }.onFailure {
                mediaMessage.value = it.message.orEmpty()
                mediaAction.value = "failed:${System.nanoTime()}"
            }
            prepared?.delete()
            mediaBusy.value = false
        }
    }

    fun load(name: String) {
        if (name.isBlank()) {
            /* Nobody to ask for. Say so (the screen's existing retry state) instead of
             * returning silently and leaving the skeleton up for ever. */
            if (user.value == null) failed.value = true
            return
        }
        viewModelScope.launch {
            loading.value = true
            failed.value = false
            runCatching { repo.profile(name) }
                .onSuccess { user.value = it }
                .onFailure { if (user.value == null) failed.value = true }
            runCatching { repo.boards(name) }.onSuccess { boards.value = it.items }
            runCatching { repo.userPins(name) }.onSuccess { pins.value = it.items }
            runCatching { repo.savedPins(name) }.onSuccess { saved.value = it.items }
            loading.value = false
        }
    }

    fun follow() {
        val current = user.value ?: return
        viewModelScope.launch {
            runCatching {
                if (current.following_me) repo.unfollow(current.id) else repo.follow(current.id)
            }.onSuccess {
                user.value = current.copy(
                    following_me = !current.following_me,
                    followers = (current.followers + if (current.following_me) -1 else 1).coerceAtLeast(0)
                )
            }
        }
    }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val items = MutableStateFlow<List<Notification>>(emptyList())

    /* Phase 8: additive UI state only — drives the notifications skeleton / retry. */
    val loading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    fun load() {
        viewModelScope.launch {
            loading.value = true
            failed.value = false
            runCatching { repo.notifications() }
                .onSuccess { items.value = it.items }
                .onFailure { if (items.value.isEmpty()) failed.value = true }
            loading.value = false
        }
    }

    fun markRead() {
        viewModelScope.launch { runCatching { repo.readNotifications() } }
    }
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val repo: PinBomRepository,
    private val session: SessionManager
) : ViewModel() {
    val items = MutableStateFlow<List<Board>>(emptyList())
    val message = MutableStateFlow("")

    /* Phase 7: additive UI state only. `lastAction` is a one-shot signal the
     * screens turn into *localized* toasts; the nonce makes two identical
     * results in a row still fire. */
    val loading = MutableStateFlow(false)
    val lastAction = MutableStateFlow("")

    fun load() {
        val name = session.username
        if (name.isBlank()) return
        viewModelScope.launch {
            loading.value = true
            runCatching { repo.boards(name) }.onSuccess { items.value = it.items }
            loading.value = false
        }
    }

    fun create(title: String, description: String) {
        viewModelScope.launch {
            runCatching { repo.createBoard(title, description) }
                .onSuccess {
                    lastAction.value = "created:${System.nanoTime()}"
                    load()
                }
                .onFailure {
                    message.value = it.message.orEmpty()
                    lastAction.value = "failed:${System.nanoTime()}"
                }
        }
    }

    fun update(id: Long, title: String, description: String) {
        viewModelScope.launch {
            runCatching { repo.patchBoard(id, title, description) }
                .onSuccess {
                    lastAction.value = "updated:${System.nanoTime()}"
                    load()
                }
                .onFailure {
                    message.value = it.message.orEmpty()
                    lastAction.value = "failed:${System.nanoTime()}"
                }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteBoard(id) }
                .onSuccess {
                    lastAction.value = "deleted:${System.nanoTime()}"
                    load()
                }
                .onFailure {
                    message.value = it.message.orEmpty()
                    lastAction.value = "failed:${System.nanoTime()}"
                }
        }
    }
}

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val item = MutableStateFlow<ir.pinbom.app.data.remote.BoardDetail?>(null)

    /* Phase 7: additive UI state only — drives the board skeleton / retry. */
    val loading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    fun load(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            loading.value = true
            failed.value = false
            runCatching { repo.board(id) }
                .onSuccess { item.value = it }
                .onFailure { if (item.value == null) failed.value = true }
            loading.value = false
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: PinBomRepository
) : ViewModel() {
    val busy = MutableStateFlow(false)

    /** Server-provided text, shown exactly as it arrives. */
    val message = MutableStateFlow("")

    /**
     * One-shot result signal ("saved:<nonce>" / "failed:<nonce>"). The screen
     * turns it into a toast worded in the current language; the nonce makes two
     * identical results in a row still fire.
     */
    val lastAction = MutableStateFlow("")

    fun save(name: String, bio: String) {
        viewModelScope.launch {
            busy.value = true
            runCatching { repo.patchMe(name, bio) }
                .onSuccess {
                    message.value = ""
                    lastAction.value = "saved:${System.nanoTime()}"
                }
                .onFailure {
                    message.value = it.message.orEmpty()
                    lastAction.value = "failed:${System.nanoTime()}"
                }
            busy.value = false
        }
    }

    fun delete(done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.deleteMe() }
                .onSuccess { done() }
                .onFailure { message.value = it.message.orEmpty() }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · upload progress
 *  One upload, followed from the button that starts it to the server that
 *  answers it. The screen hands the picked image over, the ViewModel caches a
 *  copy, queues the work and then simply mirrors WorkInfo into a value the
 *  progress card can draw — so the card survives navigation, rotation and the
 *  death of the screen that started it, while the work itself keeps running.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Which face the upload card is showing. */
enum class UploadStatus { Idle, Running, Success, Failed }

/** Everything the card draws, in one immutable value. */
data class UploadUiState(
    val status: UploadStatus = UploadStatus.Idle,
    val percent: Int = 0,
    val stage: UploadStage = UploadStage.Preparing,
    val written: Long = 0L,
    val total: Long = 0L,

    /** Shown exactly as it arrived — server wording is never re-worded. */
    val error: String? = null,

    /** The image on its way up, so the card can show what it is uploading. */
    val thumbnail: String? = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    private var observer: Job? = null
    private var workId: UUID? = null
    private var payload: Data? = null

    /** What the screen asked for, kept only so a failed *preparation* can be retried. */
    private var lastRequest: StartRequest? = null

    /**
     * Prepares the picked image, queues the upload and starts following it.
     * The six queue keys the worker always read are unchanged; [UploadKeys.FILE]
     * is the extra key that carries the prepared JPEG.
     */
    fun start(uri: Uri, title: String, description: String, tags: String, server: String, boardId: String) {
        val request = StartRequest(uri, title, description, tags, server, boardId)
        lastRequest = request
        enqueue(request)
    }

    /**
     * The publish flow, in order: decode and re-encode the picture on IO, queue the
     * work, then follow it. Nothing is queued unless there is a file to send — a
     * picture this phone cannot decode is one message to the user, not five retries
     * of an upload that could never start.
     */
    private fun enqueue(request: StartRequest) {
        val id = UUID.randomUUID()
        workId = id
        _state.value = UploadUiState(status = UploadStatus.Running, thumbnail = request.uri.toString())

        observer?.cancel()
        observer = viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) {
                runCatching {
                    ImagePreparer.prepare(
                        context = context,
                        uri = request.uri,
                        dir = ImagePreparer.uploadDir(context),
                        name = ImagePreparer.fileNameFor(id.toString()),
                    )
                }
                    .onFailure { Log.e(TAG, "could not prepare ${request.uri}", it) }
                    .getOrNull()
            }
            if (prepared == null) {
                payload = null
                _state.value = UploadUiState(
                    status = UploadStatus.Failed,
                    stage = UploadStage.Failed,
                    thumbnail = request.uri.toString(),
                    error = context.getString(R.string.upload_error_unreadable),
                )
                return@launch
            }

            val data = Data.Builder()
                .putString(UploadKeys.URI, request.uri.toString())
                .putString(UploadKeys.TITLE, request.title)
                .putString(UploadKeys.DESCRIPTION, request.description)
                .putString(UploadKeys.TAGS, request.tags)
                .putString(UploadKeys.SERVER, request.server)
                .putString(UploadKeys.BOARD_ID, request.boardId)
                .putString(UploadKeys.FILE, prepared.absolutePath)
                .build()
            payload = data

            val queued = OneTimeWorkRequestBuilder<UploadWorker>()
                .setId(id)
                .setInputData(data)
                .addTag(tag(id))
                .build()
            manager.enqueue(queued)
            manager.getWorkInfoByIdFlow(id).collect { mirror(it) }
        }
    }

    /**
     * Retry: the very same payload, queued again. A fresh id is used on purpose —
     * WorkManager keeps a finished request's row around, so re-using its id would
     * mean resurrecting a work spec instead of running a new one. The cached copy
     * is still on disk, which is why a failure never deletes it.
     */
    fun retry() {
        val data = payload
        if (data == null) {
            /* The image never became a file, so nothing was queued: start over,
             * preparation included, rather than showing a button that does nothing. */
            lastRequest?.let { enqueue(it) }
            return
        }
        val id = UUID.randomUUID()
        workId = id
        _state.value = _state.value.copy(
            status = UploadStatus.Running,
            percent = 0,
            stage = UploadStage.Preparing,
            written = 0L,
            error = null,
        )
        observer?.cancel()
        observer = viewModelScope.launch {
            val queued = OneTimeWorkRequestBuilder<UploadWorker>()
                .setId(id)
                .setInputData(data)
                .addTag(tag(id))
                .build()
            manager.enqueue(queued)
            manager.getWorkInfoByIdFlow(id).collect { mirror(it) }
        }
    }

    /** The X while uploading: stop the work and drop the half-sent copy. */
    fun cancel() {
        val id = workId ?: return
        manager.cancelWorkById(id)
        discard()
        payload = null
        _state.value = UploadUiState()
    }

    /** The X on the error card: nothing is queued any more, so just clean up. */
    fun dismiss() {
        if (_state.value.status == UploadStatus.Failed) discard()
        payload = null
        _state.value = UploadUiState()
    }

    /** WorkInfo → card state. Nothing here ever blocks the main thread. */
    private fun mirror(info: WorkInfo?) {
        if (info == null) return
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> _state.value = _state.value.copy(
                status = UploadStatus.Running,
                percent = 0,
                stage = UploadStage.Preparing,
                written = 0L,
                error = null,
            )

            WorkInfo.State.RUNNING -> _state.value = _state.value.copy(
                status = UploadStatus.Running,
                /* 100 % belongs to the server's answer, never to the socket. */
                percent = info.progress.getInt(UploadKeys.PROGRESS, 0).coerceIn(0, 99),
                stage = UploadStage.of(info.progress.getString(UploadKeys.STAGE)),
                written = info.progress.getLong(UploadKeys.WRITTEN, 0L),
                total = info.progress.getLong(UploadKeys.TOTAL, 0L),
                error = null,
            )

            WorkInfo.State.SUCCEEDED -> {
                if (_state.value.status == UploadStatus.Success) return
                _state.value = _state.value.copy(
                    status = UploadStatus.Success,
                    percent = 100,
                    stage = UploadStage.Done,
                    written = _state.value.total,
                    error = null,
                )
                UploadEvents.publishCompleted()
                viewModelScope.launch {
                    /* Long enough to read the check mark, short enough not to nag. */
                    delay(SUCCESS_VISIBLE_MS)
                    if (_state.value.status == UploadStatus.Success) _state.value = UploadUiState()
                }
            }

            WorkInfo.State.FAILED -> _state.value = _state.value.copy(
                status = UploadStatus.Failed,
                stage = UploadStage.Failed,
                error = info.outputData.getString(UploadKeys.ERROR),
            )

            WorkInfo.State.CANCELLED -> _state.value = UploadUiState()
        }
    }

    private val manager get() = WorkManager.getInstance(context)

    private fun tag(id: UUID): String = "$TAG:$id"

    /**
     * Deletes the prepared copy. It is named by the payload, which is what keeps a
     * retry's cache file collectable: the retry gets a fresh work id, but the file
     * it re-sends still carries the id it was written under.
     */
    private fun discard() {
        val named = payload?.getString(UploadKeys.FILE)
        val id = workId?.toString()
        viewModelScope.launch(Dispatchers.IO) {
            named?.let { path -> runCatching { File(path).delete() } }
            id?.let { runCatching { File(ImagePreparer.uploadDir(context), ImagePreparer.fileNameFor(it)).delete() } }
        }
    }

    /** One publish, as the screen described it. */
    private data class StartRequest(
        val uri: Uri,
        val title: String,
        val description: String,
        val tags: String,
        val server: String,
        val boardId: String,
    )

    private companion object {
        const val TAG = "pinbom_upload"
        const val SUCCESS_VISIBLE_MS = 1_800L
    }
}

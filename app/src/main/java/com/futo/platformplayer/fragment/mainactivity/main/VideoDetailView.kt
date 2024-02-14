package com.futo.platformplayer.fragment.mainactivity.main

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Browser
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.util.Rational
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.TimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.futo.platformplayer.R
import com.futo.platformplayer.Settings
import com.futo.platformplayer.UIDialogs
import com.futo.platformplayer.UISlideOverlays
import com.futo.platformplayer.api.media.IPluginSourced
import com.futo.platformplayer.api.media.LiveChatManager
import com.futo.platformplayer.api.media.PlatformID
import com.futo.platformplayer.api.media.exceptions.ContentNotAvailableYetException
import com.futo.platformplayer.api.media.exceptions.NoPlatformClientException
import com.futo.platformplayer.api.media.models.PlatformAuthorMembershipLink
import com.futo.platformplayer.api.media.models.chapters.ChapterType
import com.futo.platformplayer.api.media.models.chapters.IChapter
import com.futo.platformplayer.api.media.models.comments.PolycentricPlatformComment
import com.futo.platformplayer.api.media.models.live.ILiveChatWindowDescriptor
import com.futo.platformplayer.api.media.models.live.IPlatformLiveEvent
import com.futo.platformplayer.api.media.models.playback.IPlaybackTracker
import com.futo.platformplayer.api.media.models.ratings.RatingLikeDislikes
import com.futo.platformplayer.api.media.models.ratings.RatingLikes
import com.futo.platformplayer.api.media.models.streams.VideoUnMuxedSourceDescriptor
import com.futo.platformplayer.api.media.models.streams.sources.IAudioSource
import com.futo.platformplayer.api.media.models.streams.sources.IDashManifestSource
import com.futo.platformplayer.api.media.models.streams.sources.IHLSManifestSource
import com.futo.platformplayer.api.media.models.streams.sources.IVideoSource
import com.futo.platformplayer.api.media.models.streams.sources.LocalAudioSource
import com.futo.platformplayer.api.media.models.streams.sources.LocalSubtitleSource
import com.futo.platformplayer.api.media.models.streams.sources.LocalVideoSource
import com.futo.platformplayer.api.media.models.subtitles.ISubtitleSource
import com.futo.platformplayer.api.media.models.video.IPlatformVideo
import com.futo.platformplayer.api.media.models.video.IPlatformVideoDetails
import com.futo.platformplayer.api.media.models.video.SerializedPlatformVideo
import com.futo.platformplayer.api.media.platforms.js.SourcePluginConfig
import com.futo.platformplayer.api.media.platforms.js.models.JSVideoDetails
import com.futo.platformplayer.api.media.structures.IPager
import com.futo.platformplayer.casting.CastConnectionState
import com.futo.platformplayer.casting.StateCasting
import com.futo.platformplayer.constructs.Event0
import com.futo.platformplayer.constructs.Event1
import com.futo.platformplayer.constructs.TaskHandler
import com.futo.platformplayer.downloads.VideoLocal
import com.futo.platformplayer.dp
import com.futo.platformplayer.engine.exceptions.ScriptAgeException
import com.futo.platformplayer.engine.exceptions.ScriptException
import com.futo.platformplayer.engine.exceptions.ScriptImplementationException
import com.futo.platformplayer.engine.exceptions.ScriptLoginRequiredException
import com.futo.platformplayer.engine.exceptions.ScriptUnavailableException
import com.futo.platformplayer.exceptions.UnsupportedCastException
import com.futo.platformplayer.fixHtmlLinks
import com.futo.platformplayer.fixHtmlWhitespace
import com.futo.platformplayer.fullyBackfillServersAnnounceExceptions
import com.futo.platformplayer.getNowDiffSeconds
import com.futo.platformplayer.helpers.VideoHelper
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.models.Subscription
import com.futo.platformplayer.polycentric.PolycentricCache
import com.futo.platformplayer.receivers.MediaControlReceiver
import com.futo.platformplayer.selectBestImage
import com.futo.platformplayer.states.AnnouncementType
import com.futo.platformplayer.states.StateAnnouncement
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StateDownloads
import com.futo.platformplayer.states.StateHistory
import com.futo.platformplayer.states.StatePlatform
import com.futo.platformplayer.states.StatePlayer
import com.futo.platformplayer.states.StatePlaylists
import com.futo.platformplayer.states.StatePlugins
import com.futo.platformplayer.states.StatePolycentric
import com.futo.platformplayer.states.StateSubscriptions
import com.futo.platformplayer.stores.FragmentedStorage
import com.futo.platformplayer.stores.StringArrayStorage
import com.futo.platformplayer.stores.db.types.DBHistory
import com.futo.platformplayer.toHumanBitrate
import com.futo.platformplayer.toHumanNowDiffString
import com.futo.platformplayer.toHumanNumber
import com.futo.platformplayer.toHumanTime
import com.futo.platformplayer.views.MonetizationView
import com.futo.platformplayer.views.behavior.TouchInterceptFrameLayout
import com.futo.platformplayer.views.casting.CastView
import com.futo.platformplayer.views.comments.AddCommentView
import com.futo.platformplayer.views.others.CreatorThumbnail
import com.futo.platformplayer.views.others.Toggle
import com.futo.platformplayer.views.overlays.DescriptionOverlay
import com.futo.platformplayer.views.overlays.LiveChatOverlay
import com.futo.platformplayer.views.overlays.QueueEditorOverlay
import com.futo.platformplayer.views.overlays.RepliesOverlay
import com.futo.platformplayer.views.overlays.SupportOverlay
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuButtonList
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuGroup
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuItem
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuOverlay
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuTitle
import com.futo.platformplayer.views.pills.PillRatingLikesDislikes
import com.futo.platformplayer.views.pills.RoundButton
import com.futo.platformplayer.views.pills.RoundButtonGroup
import com.futo.platformplayer.views.platform.PlatformIndicator
import com.futo.platformplayer.views.segments.CommentsList
import com.futo.platformplayer.views.subscriptions.SubscribeButton
import com.futo.platformplayer.views.video.FutoVideoPlayer
import com.futo.platformplayer.views.video.FutoVideoPlayerBase
import com.futo.platformplayer.views.videometa.UpNextView
import com.futo.polycentric.core.ApiMethods
import com.futo.polycentric.core.ContentType
import com.futo.polycentric.core.Models
import com.futo.polycentric.core.Opinion
import com.futo.polycentric.core.toURLInfoSystemLinkUrl
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import userpackage.Protocol
import java.time.OffsetDateTime
import kotlin.math.abs
import kotlin.math.roundToLong

class VideoDetailView : ConstraintLayout {
    private val TAG = "VideoDetailView"

    lateinit var fragment: VideoDetailFragment;

    private var _destroyed = false;

    private var _url: String? = null;
    private var _playWhenReady = true;
    private var _searchVideo: IPlatformVideo? = null;
    var video: IPlatformVideoDetails? = null
        private set;
    var videoLocal: VideoLocal? = null;
    private var _playbackTracker: IPlaybackTracker? = null;
    private var _historyIndex: DBHistory.Index? = null;

    val currentUrl get() = video?.url ?: _searchVideo?.url ?: _url;

    private var _liveChat: LiveChatManager? = null;
    private var _videoResumePositionMilliseconds : Long = 0L;

    private val _player: FutoVideoPlayer;
    private val _cast: CastView;
    private val _playerProgress: PlayerControlView;
    private val _timeBar: TimeBar;
    private var _upNext: UpNextView;

    val rootView: ConstraintLayout;

    private val _title: TextView;
    private val _subTitle: TextView;
    private val _description: TextView;
    private val _descriptionContainer: LinearLayout;

    private val _buttonToggleAlternativeMetadata: ImageButton;
    private val _buttonToggleAlternativeMetadataMinimized: ImageButton;

    private val _platform: PlatformIndicator;

    private val _channelName: TextView;
    private val _channelMeta: TextView;
    private val _creatorThumbnail: CreatorThumbnail;
    private val _channelButton: LinearLayout;

    private val _description_viewMore: TextView;

    private val _overlay_loading: FrameLayout;
    private val _overlay_loading_spinner: ImageView;
    private val _rating: PillRatingLikesDislikes;

    private val _minimize_controls: LinearLayout;
    private val _minimize_controls_play: ImageButton;
    private val _minimize_controls_pause: ImageButton;
    private val _minimize_controls_close: ImageButton;
    private val _minimize_title: TextView;
    private val _minimize_meta: TextView;

    private val _commentsList: CommentsList;

    private var _minimizeProgress: Float = 0f;
    private val _buttonSubscribe: SubscribeButton;

    private val _buttonPins: RoundButtonGroup;
    //private val _buttonMore: RoundButton;

    var preventPictureInPicture: Boolean = false;

    private val _textComments: TextView;
    private val _textCommentType: TextView;
    private val _addCommentView: AddCommentView;
    private val _toggleCommentType: Toggle;

    private val _layoutSkip: LinearLayout;
    private val _textSkip: TextView;
    private val _textResume: TextView;
    private val _layoutResume: LinearLayout;
    private var _jobHideResume: Job? = null;
    private val _layoutPlayerContainer: TouchInterceptFrameLayout;

    //Overlays
    private val _overlayContainer: FrameLayout;
    private val _overlay_quality_container: FrameLayout;
    private var _overlay_quality_selector: SlideUpMenuOverlay? = null;

    //Bottom Containers
    private val _container_content: FrameLayout;
    private val _container_content_main: FrameLayout;
    private val _container_content_queue: QueueEditorOverlay;
    private val _container_content_replies: RepliesOverlay;
    private val _container_content_description: DescriptionOverlay;
    private val _container_content_liveChat: LiveChatOverlay;
    private val _container_content_support: SupportOverlay;

    private var _container_content_current: View;

    private val _textLikes: TextView;
    private val _textDislikes: TextView;
    private val _layoutRating: LinearLayout;
    private val _imageDislikeIcon: ImageView;
    private val _imageLikeIcon: ImageView;
    private val _layoutToggleCommentSection: LinearLayout;

    private val _monetization: MonetizationView;

    private val _buttonMore: RoundButton;

    private var _didStop: Boolean = false;
    private var _onPauseCalled = false;
    private var _lastVideoSource: IVideoSource? = null;
    private var _lastAudioSource: IAudioSource? = null;
    private var _lastSubtitleSource: ISubtitleSource? = null;
    private var _isCasting: Boolean = false;
    private var _isAlternativeMetadataShown: Boolean = Settings.instance.browsing.showAlternativeMetadataByDefault;

    var isPlaying: Boolean = false
        private set;
    var lastPositionMilliseconds: Long = 0
        private set;
    private var _historicalPosition: Long = 0;
    private var _commentsCount = 0;
    private var _polycentricProfile: PolycentricCache.CachedPolycentricProfile? = null;
    private var _slideUpOverlay: SlideUpMenuOverlay? = null;

    //Events
    val onMinimize = Event0();
    val onMaximize = Event1<Boolean>();
    val onClose = Event0();
    val onFullscreenChanged = Event1<Boolean>();
    val onEnterPictureInPicture = Event0();
    val onPlayChanged = Event1<Boolean>();

    var allowBackground : Boolean = false
        private set;

    val onTouchCancel = Event0();
    private var _lastPositionSaveTime: Long = -1;

    private val DP_5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics);
    private val DP_2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics);

    private var _retryJob: Job? = null;
    private var _retryCount = 0;
    //TODO: Determine better behavior, waiting 60 seconds for an error that is guaranteed to happen is a bit much. (Needed? If so, maybe need special UI for retrying)
    private val _retryIntervals: Array<Long> = arrayOf(1, 1);//2, 4, 8, 16, 32);

    private var _liveTryJob: Job? = null;
    private val _liveStreamCheckInterval = listOf(
        Pair(-10 * 60, 5 * 60), //around 10 minutes, try every 5 minute
        Pair(-5 * 60, 30), //around 5 minutes, try every 30 seconds
        Pair(0, 10) //around live, try every 10 seconds
    );

    @androidx.annotation.OptIn(UnstableApi::class)
    constructor(context: Context, attrs : AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.fragview_video_detail, this);

        //Declare Views
        rootView = findViewById(R.id.videodetail_root);
        _cast = findViewById(R.id.videodetail_cast);
        _player = findViewById(R.id.videodetail_player);
        _playerProgress = findViewById(R.id.videodetail_progress);
        _timeBar = _playerProgress.findViewById(androidx.media3.ui.R.id.exo_progress);
        _title = findViewById(R.id.videodetail_title);
        _subTitle = findViewById(R.id.videodetail_meta);
        _platform = findViewById(R.id.videodetail_platform);
        _buttonToggleAlternativeMetadata = findViewById(R.id.button_toggle_alternative_metadata);
        _buttonToggleAlternativeMetadataMinimized = findViewById(R.id.button_toggle_alternative_metadata_minimized);

        _description = findViewById(R.id.videodetail_description);
        _descriptionContainer = findViewById(R.id.videodetail_description_container);
        _channelName = findViewById(R.id.videodetail_channel_name);
        _channelMeta = findViewById(R.id.videodetail_channel_meta);
        _creatorThumbnail = findViewById(R.id.creator_thumbnail);
        _channelButton = findViewById(R.id.videodetail_channel_button);
        _description_viewMore = findViewById(R.id.videodetail_description_view_more);
        _overlay_loading = findViewById(R.id.videodetail_loading_overlay);
        _overlay_loading_spinner = findViewById(R.id.videodetail_loader);
        _rating = findViewById(R.id.videodetail_rating);
        _upNext = findViewById(R.id.up_next);
        _textCommentType = findViewById(R.id.text_comment_type);
        _toggleCommentType = findViewById(R.id.toggle_comment_type);
        _layoutToggleCommentSection = findViewById(R.id.layout_toggle_comment_section);

        _overlayContainer = findViewById(R.id.overlay_container);
        _overlay_quality_container = findViewById(R.id.videodetail_quality_overview);

        _minimize_controls = findViewById(R.id.minimize_controls);
        _minimize_controls_pause = findViewById(R.id.minimize_pause);
        _minimize_controls_close = findViewById(R.id.minimize_close);
        _minimize_controls_play = findViewById(R.id.minimize_play);
        _minimize_title = findViewById(R.id.videodetail_title_minimized);
        _minimize_meta = findViewById(R.id.videodetail_meta_minimized);
        _buttonSubscribe = findViewById(R.id.button_subscribe);

        _container_content = findViewById(R.id.contentContainer);
        _container_content_main = findViewById(R.id.videodetail_container_main);
        _container_content_queue = findViewById(R.id.videodetail_container_queue);
        _container_content_replies = findViewById(R.id.videodetail_container_replies);
        _container_content_description = findViewById(R.id.videodetail_container_description);
        _container_content_liveChat = findViewById(R.id.videodetail_container_livechat);
        _container_content_support = findViewById(R.id.videodetail_container_support)

        _textComments = findViewById(R.id.text_comments);
        _addCommentView = findViewById(R.id.add_comment_view);
        _commentsList = findViewById(R.id.comments_list);

        _layoutSkip = findViewById(R.id.layout_skip);
        _textSkip = findViewById(R.id.text_skip);
        _layoutResume = findViewById(R.id.layout_resume);
        _textResume = findViewById(R.id.text_resume);
        _layoutPlayerContainer = findViewById(R.id.layout_player_container);
        _layoutPlayerContainer.onClick.subscribe { onMaximize.emit(false); };

        _layoutRating = findViewById(R.id.layout_rating);
        _textDislikes = findViewById(R.id.text_dislikes);
        _textLikes = findViewById(R.id.text_likes);
        _imageLikeIcon = findViewById(R.id.image_like_icon);
        _imageDislikeIcon = findViewById(R.id.image_dislike_icon);

        _monetization = findViewById(R.id.monetization);
        _player.attachPlayer();


        _buttonToggleAlternativeMetadata.setOnClickListener(::toggleAlternativeMetadata);
        _buttonToggleAlternativeMetadataMinimized.setOnClickListener(:: toggleAlternativeMetadata);

        _buttonSubscribe.onSubscribed.subscribe {
            _slideUpOverlay = UISlideOverlays.showSubscriptionOptionsOverlay(it, _overlayContainer);
        };

        _container_content_liveChat.onRaidNow.subscribe {
            StatePlayer.instance.clearQueue();
            fragment.navigate<VideoDetailFragment>(it.targetUrl);
        };

        _monetization.onSupportTap.subscribe {
            _container_content_support.setPolycentricProfile(_polycentricProfile?.profile);
            switchContentView(_container_content_support);
        };

        _monetization.onStoreTap.subscribe {
            _polycentricProfile?.profile?.systemState?.store?.let {
                try {
                    val uri = Uri.parse(it);
                    val intent = Intent(Intent.ACTION_VIEW);
                    intent.data = uri;
                    context.startActivity(intent);
                } catch (e: Throwable) {
                    Logger.e(TAG, "Failed to open URI: '${it}'.", e);
                }
            }
        };
        _monetization.onUrlTap.subscribe {
            fragment.navigate<BrowserFragment>(it);
            onMinimize.emit();
        }

        _player.attachPlayer();

        _container_content_liveChat.onRaidNow.subscribe {
            StatePlayer.instance.clearQueue();
            fragment.navigate<VideoDetailFragment>(it.targetUrl);
        };

        StateApp.instance.preventPictureInPicture.subscribe(this) {
            Logger.i(TAG, "StateApp.instance.preventPictureInPicture.subscribe preventPictureInPicture = true");
            preventPictureInPicture = true;
        };

        _addCommentView.onCommentAdded.subscribe {
            _commentsList.addComment(it);
        }

        _commentsList.onCommentsLoaded.subscribe { count ->
            _commentsCount = count;
            updateCommentType(false);
        };

        _toggleCommentType.onValueChanged.subscribe {
            updateCommentType(true);
        };

        _textCommentType.setOnClickListener {
            _toggleCommentType.setValue(!_toggleCommentType.value, true);
            updateCommentType(true);
        };

        val layoutTop: LinearLayout = findViewById(R.id.layout_top);
        _container_content_main.removeView(layoutTop);
        _commentsList.setPrependedView(layoutTop);

        _buttonPins = layoutTop.findViewById(R.id.buttons_pins);
        _buttonPins.alwaysShowLastButton = true;

        var buttonMore: RoundButton? = null;
        buttonMore = RoundButton(context, R.drawable.ic_menu, context.getString(R.string.more), TAG_MORE) {
            _slideUpOverlay = UISlideOverlays.showMoreButtonOverlay(_overlayContainer, _buttonPins, listOf(TAG_MORE), false) {selected ->
                _buttonPins.setButtons(*(selected + listOf(buttonMore!!)).toTypedArray());
                _buttonPinStore.set(*selected.filter { it.tagRef is String }.map{ it.tagRef as String }.toTypedArray())
                _buttonPinStore.save();
            };
        };
        _buttonMore = buttonMore;
        updateMoreButtons();

        _channelButton.setOnClickListener {
            if (video is TutorialFragment.TutorialVideo) {
                return@setOnClickListener
            }

            (video?.author ?: _searchVideo?.author)?.let {
                fragment.navigate<ChannelFragment>(it);
                fragment.lifecycleScope.launch {
                    delay(100);
                    fragment.minimizeVideoDetail();
                };
            };
        };

        _rating.visibility = View.GONE;

        _cast.onSettingsClick.subscribe { showVideoSettings() };
        _player.onVideoSettings.subscribe { showVideoSettings() };
        _player.onToggleFullScreen.subscribe(::handleFullScreen);

        val onChapterChanged = { chapter: IChapter?, isScrub: Boolean ->
            if(_layoutSkip.visibility == VISIBLE && chapter?.type != ChapterType.SKIPPABLE)
                _layoutSkip.visibility = GONE;

            if(!isScrub) {
                if(chapter?.type == ChapterType.SKIPPABLE) {
                    _layoutSkip.visibility = VISIBLE;
                } else if(chapter?.type == ChapterType.SKIP || chapter?.type == ChapterType.SKIPONCE) {
                    val ad = StateCasting.instance.activeDevice
                    if (ad != null) {
                        ad.seekVideo(chapter.timeEnd)
                    } else {
                        _player.seekTo((chapter.timeEnd * 1000).toLong());
                    }

                    UIDialogs.toast(context, "Skipped chapter [${chapter.name}]", false);
                }
            }
        };

        _player.onChapterChanged.subscribe(onChapterChanged);
        _cast.onChapterChanged.subscribe(onChapterChanged);

        _cast.onMinimizeClick.subscribe {
            _player.setFullScreen(false);
            onMinimize.emit();
        };
        _player.onMinimize.subscribe {
            _player.setFullScreen(false);
            onMinimize.emit();
        };

        _player.onTimeBarChanged.subscribe { position, _ ->
            if (!_isCasting && !_didStop) {
                setLastPositionMilliseconds(position, true);
            }
            updatePlaybackTracking(position);
        };

        _player.onVideoClicked.subscribe {
            if(_minimizeProgress < 0.5)
                onMaximize.emit(false);
        }
        _player.onSourceChanged.subscribe(::onSourceChanged);
        _player.onSourceEnded.subscribe {
            if (!fragment.isInPictureInPicture) {
                _player.gestureControl.showControls(false);
            }

            _player.setIsReplay(true);

            val searchVideo = StatePlayer.instance.getCurrentQueueItem();
            if (searchVideo is SerializedPlatformVideo?) {
                searchVideo?.let { StatePlaylists.instance.removeFromWatchLater(it) };
            }

            nextVideo();
        };
        _player.onDatasourceError.subscribe(::onDataSourceError);
        _player.onNext.subscribe { nextVideo(true, true, true) };
        _player.onPrevious.subscribe { prevVideo(true) };
        _cast.onPrevious.subscribe { prevVideo(true) };
        _cast.onNext.subscribe { nextVideo(true, true, true) };

        _minimize_controls_play.setOnClickListener { handlePlay(); };
        _minimize_controls_pause.setOnClickListener { handlePause(); };
        _minimize_controls_close.setOnClickListener { onClose.emit(); };
        _minimize_title.setOnClickListener { onMaximize.emit(false) };
        _minimize_meta.setOnClickListener { onMaximize.emit(false) };

        _player.onPlayChanged.subscribe {
            if (StateCasting.instance.activeDevice == null) {
                handlePlayChanged(it);
            }
        };

        if (!isInEditMode) {
            StateCasting.instance.onActiveDeviceConnectionStateChanged.subscribe(this) { _, connectionState ->
                if (_onPauseCalled) {
                    return@subscribe;
                }

                when (connectionState) {
                    CastConnectionState.CONNECTED -> {
                        loadCurrentVideo(lastPositionMilliseconds);
                        updatePillButtonVisibilities();
                        setCastEnabled(true);
                    }
                    CastConnectionState.DISCONNECTED -> {
                        loadCurrentVideo(lastPositionMilliseconds);
                        updatePillButtonVisibilities();
                        setCastEnabled(false);
                    }
                    else -> {}
                }
            };

            updatePillButtonVisibilities();

            StateCasting.instance.onActiveDevicePlayChanged.subscribe(this) {
                val activeDevice = StateCasting.instance.activeDevice;
                if (activeDevice != null) {
                    handlePlayChanged(it);

                    val v = video;
                    if (!it && v != null && v.duration - activeDevice.time.toLong() < 2L) {
                        nextVideo();
                    }
                }
            };

            StateCasting.instance.onActiveDeviceTimeChanged.subscribe(this) {
                if (_isCasting) {
                    setLastPositionMilliseconds((it * 1000.0).toLong(), true);
                    _cast.setTime(lastPositionMilliseconds);
                    _timeBar.setPosition(it.toLong());
                    _timeBar.setBufferedPosition(0);
                    _timeBar.setDuration(video?.duration ?: 0);
                }
            };
        }

        _playerProgress.player = _player.exoPlayer?.player;
        _playerProgress.setProgressUpdateListener { position, _ ->
            StatePlayer.instance.updateMediaSessionPlaybackState(_player.exoPlayer?.getPlaybackStateCompat() ?: PlaybackStateCompat.STATE_NONE, position);
        };

        StatePlayer.instance.onQueueChanged.subscribe(this) {
            if(!_destroyed) {
                updateQueueState();
                StatePlayer.instance.updateMediaSession(null);
                _cast.setLoopVisible(!StatePlayer.instance.hasQueue);
            }
        };
        StatePlayer.instance.onVideoChanging.subscribe(this) {
            setVideoOverview(it);
        };
        MediaControlReceiver.onLowerVolumeReceived.subscribe(this) { handleLowerVolume() };
        MediaControlReceiver.onPlayReceived.subscribe(this) { handlePlay() };
        MediaControlReceiver.onPauseReceived.subscribe(this) { handlePause() };
        MediaControlReceiver.onNextReceived.subscribe(this) { nextVideo(true, true, true) };
        MediaControlReceiver.onPreviousReceived.subscribe(this) { prevVideo(true) };
        MediaControlReceiver.onCloseReceived.subscribe(this) {
            Logger.i(TAG, "MediaControlReceiver.onCloseReceived")
            onClose.emit()
        };
        MediaControlReceiver.onSeekToReceived.subscribe(this) { handleSeek(it); };

        _container_content_description.onClose.subscribe { switchContentView(_container_content_main); };
        _container_content_liveChat.onClose.subscribe { switchContentView(_container_content_main); };
        _container_content_queue.onClose.subscribe { switchContentView(_container_content_main); };
        _container_content_replies.onClose.subscribe { switchContentView(_container_content_main); };
        _container_content_support.onClose.subscribe { switchContentView(_container_content_main); };

        _description_viewMore.setOnClickListener {
            switchContentView(_container_content_description);
        };

        _upNext.onNextItem.subscribe {
            nextVideo(true, true, true);
        };
        _upNext.onOpenQueueClick.subscribe {
            _container_content_queue.updateQueue();
            switchContentView(_container_content_queue);
        };
        _upNext.onRestartQueue.subscribe {
            val item = StatePlayer.instance.restartQueue();
            if(item != null)
                setVideoOverview(item, true);
        };

        _container_content_current = _container_content_main;

        _commentsList.onRepliesClick.subscribe { c ->
            val replyCount = c.replyCount ?: 0;
            var metadata = "";
            if (replyCount > 0) {
                metadata += "$replyCount " + context.getString(R.string.replies);
            }

            if (c is PolycentricPlatformComment) {
                var parentComment: PolycentricPlatformComment = c;
                _container_content_replies.load(_toggleCommentType.value, metadata, c.contextUrl, c.reference, c,
                    { StatePolycentric.instance.getCommentPager(c.contextUrl, c.reference) },
                    {
                        val newComment = parentComment.cloneWithUpdatedReplyCount((parentComment.replyCount ?: 0) + 1);
                        _commentsList.replaceComment(parentComment, newComment);
                        parentComment = newComment;
                    });
            } else {
                _container_content_replies.load(_toggleCommentType.value, metadata, null, null, c, { StatePlatform.instance.getSubComments(c) });
            }
            switchContentView(_container_content_replies);
        };

        onClose.subscribe {
            _lastVideoSource = null;
            _lastAudioSource = null;
            _lastSubtitleSource = null;
            video = null;
            _playbackTracker = null;
            Logger.i(TAG, "Keep screen on unset onClose")
            fragment.activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        };

        _layoutResume.setOnClickListener {
            handleSeek(_historicalPosition * 1000);

            val job = _jobHideResume;
            _jobHideResume = null;
            job?.cancel();

            _layoutResume.visibility = View.GONE;
        };

        _layoutSkip.setOnClickListener {
            val ad = StateCasting.instance.activeDevice;
            if (ad != null) {
                val currentChapter = _cast.getCurrentChapter((ad.time * 1000).toLong());
                if(currentChapter?.type == ChapterType.SKIPPABLE) {
                    ad.seekVideo(currentChapter.timeEnd);
                }
            } else {
                val currentChapter = _player.getCurrentChapter(_player.position);
                if(currentChapter?.type == ChapterType.SKIPPABLE) {
                    _player.seekTo((currentChapter.timeEnd * 1000).toLong());
                }
            }
        }
    }

    val _trackingUpdateTimeLock = Object();
    val _trackingUpdateInterval = 2500;
    var _trackingLastUpdateTime = System.currentTimeMillis();
    var _trackingLastPosition: Long = 0;
    var _trackingLastVideo: IPlatformVideoDetails? = null;
    var _trackingTotalWatched: Long = 0;
    var _trackingDidCountView: Boolean = false;
    var _trackingLastVideoSubscription: Subscription? = null;
    fun updatePlaybackTracking(position: Long) {
        if(!Settings.instance.subscriptions.allowPlaytimeTracking)
            return;
        val now = System.currentTimeMillis();
        val shouldUpdate = synchronized(_trackingUpdateTimeLock) {
            val doUpdate = (now - _trackingLastUpdateTime) > _trackingUpdateInterval;
            if(doUpdate)
                _trackingLastUpdateTime = now;
            return@synchronized doUpdate;
        }
        if(shouldUpdate) {
            val currentVideo = video;
            val delta = position - _trackingLastPosition;
            _trackingLastPosition = position;

            if(currentVideo != null && currentVideo == _trackingLastVideo) {
                if(delta > 500 && delta < _trackingUpdateInterval * 1.5) {
                    _trackingLastVideoSubscription?.let {
                        Logger.i(TAG, "Subscription [${it.channel.name}] watch time delta [${delta}]" +
                                "(${"%.2f".format((_trackingTotalWatched / 1000) / currentVideo.duration.toDouble().coerceAtLeast(1.0))})");
                        it.updatePlayback((delta / 1000).toInt());
                        _trackingTotalWatched += delta;
                        if(!_trackingDidCountView && currentVideo.duration > 0) {
                            val percentage = (_trackingTotalWatched / 1000) / currentVideo.duration.toDouble();
                            if(percentage > 0.4) {
                                Logger.i(TAG, "Subscription [${it.channel.name}] new view");
                                _trackingDidCountView = true;
                                it.addPlaybackView();
                            }
                        }
                        it.saveAsync();
                    };
                }
            }
            else {
                if(_trackingLastVideo == null && currentVideo == null)
                    return;
                _trackingLastVideo = currentVideo;
                _trackingTotalWatched = 0;
                if(currentVideo?.author?.url != null)
                    _trackingLastVideoSubscription = StateSubscriptions.instance.getSubscription(currentVideo.author.url);
                else
                    _trackingLastVideoSubscription = null;
            }
        }
    }

    fun stopAllGestures() {
        _player.stopAllGestures();
        _cast.stopAllGestures();
    }

    fun updateMoreButtons() {
        val buttons = listOf(RoundButton(context, R.drawable.ic_add, context.getString(R.string.add), TAG_ADD) {
            (video ?: _searchVideo)?.let {
                _slideUpOverlay = UISlideOverlays.showAddToOverlay(it, _overlayContainer) {
                    _slideUpOverlay = it
                };
            }
        },
            if(video?.isLive ?: false)
                RoundButton(context, R.drawable.ic_chat, context.getString(R.string.live_chat), TAG_LIVECHAT) {
                    video?.let {
                        try {
                            loadLiveChat(it);
                        }
                        catch(ex: Throwable) {
                            Logger.e(TAG, "Failed to reopen live chat", ex);
                        }
                    }
                    _slideUpOverlay?.hide();
                } else null,
            RoundButton(context, R.drawable.ic_screen_share, context.getString(R.string.background), TAG_BACKGROUND) {
                if(!allowBackground) {
                    _player.switchToAudioMode();
                    allowBackground = true;
                    it.text.text = resources.getString(R.string.background_revert);
                }
                else {
                    _player.switchToVideoMode();
                    allowBackground = false;
                    it.text.text = resources.getString(R.string.background);
                }
                _slideUpOverlay?.hide();
            },
            RoundButton(context, R.drawable.ic_download, context.getString(R.string.download), TAG_DOWNLOAD) {
                video?.let {
                    _slideUpOverlay = UISlideOverlays.showDownloadVideoOverlay(it, _overlayContainer, context.contentResolver);
                };
            },
            RoundButton(context, R.drawable.ic_share, context.getString(R.string.share), TAG_SHARE) {
                video?.let {
                    Logger.i(TAG, "Share preventPictureInPicture = true");
                    preventPictureInPicture = true;
                    shareVideo();
                };
                _slideUpOverlay?.hide();
            },
            RoundButton(context, R.drawable.ic_screen_share, context.getString(R.string.overlay), TAG_OVERLAY) {
                this.startPictureInPicture();
                fragment.forcePictureInPicture();
                //PiPActivity.startPiP(context);
                _slideUpOverlay?.hide();
            },
            RoundButton(context, R.drawable.ic_export, context.getString(R.string.page), TAG_OPEN) {
                video?.let {
                    val url = video?.shareUrl ?: _searchVideo?.shareUrl ?: _url;
                    fragment.navigate<BrowserFragment>(url);
                    fragment.minimizeVideoDetail();
                };
                _slideUpOverlay?.hide();
            },
            RoundButton(context, R.drawable.ic_refresh, context.getString(R.string.reload), "Reload") {
                reloadVideo();
                _slideUpOverlay?.hide();
            }).filterNotNull();
        if(!_buttonPinStore.getAllValues().any())
            _buttonPins.setButtons(*(buttons + listOf(_buttonMore)).toTypedArray());
        else {
            val selectedButtons = _buttonPinStore.getAllValues()
                .map { x-> buttons.find { it.tagRef == x } }
                .filter { it != null }
                .map { it!! };
            _buttonPins.setButtons(*(selectedButtons +
                    buttons.filter { !selectedButtons.contains(it) } +
                    listOf(_buttonMore)).toTypedArray());
        }
    }

    fun reloadVideo() {
        fragment.lifecycleScope.launch (Dispatchers.IO) {
            video?.let {
                Logger.i(TAG, "Reloading video");
                try {
                    val video = StatePlatform.instance.getContentDetails(it.url, true).await();
                    if(video !is IPlatformVideoDetails)
                        throw IllegalStateException("Expected media content, found ${video.contentType}");

                    withContext(Dispatchers.Main) {
                        setVideoDetails(video);
                    }
                }
                catch(ex: Throwable) {
                    withContext(Dispatchers.Main) {
                        UIDialogs.showGeneralErrorDialog(context, ex.message ?: "", ex);
                    }
                }
            }
        }
    }


    private val _historyIndexLock = Mutex(false);
    suspend fun getHistoryIndex(video: IPlatformVideo): DBHistory.Index? = withContext(Dispatchers.IO){
        _historyIndexLock.withLock {
            val current = _historyIndex;
            if(current == null || current.url != video.url) {
                val index = StateHistory.instance.getHistoryByVideo(video, true);
                _historyIndex = index;
                return@withContext index;
            }
            return@withContext current;
        }
    }


    //Lifecycle
    fun onResume() {
        Logger.v(TAG, "onResume");
        _onPauseCalled = false;

        Logger.i(TAG, "_video: ${video?.name ?: "no video"}");
        Logger.i(TAG, "_didStop: $_didStop");

        //Recover cancelled loads
        if(video == null) {
            val t = (lastPositionMilliseconds / 1000.0f).roundToLong();
            if(_searchVideo != null)
                setVideoOverview(_searchVideo!!, true, t);
            else if(_url != null)
                setVideo(_url!!, t, _playWhenReady);
        }
        else if(_didStop) {
            _didStop = false;
            Logger.i(TAG, "loadCurrentVideo _lastPosition=${lastPositionMilliseconds}");
            loadCurrentVideo(lastPositionMilliseconds);
            handlePause();
        }

        if(_player.isAudioMode) {
            //Requested behavior to leave it in audio mode. leaving it commented if it causes issues, revert?
            if(!allowBackground) {
                _player.switchToVideoMode();
                _buttonPins.getButtonByTag(TAG_BACKGROUND)?.text?.text = resources.getString(R.string.background);
            }
        }
        if(!_player.isFitMode && !_player.isFullScreen && !fragment.isInPictureInPicture)
            _player.fitHeight();

        _player.updateRotateLock();
    }
    fun onPause() {
        Logger.v(TAG, "onPause");

        _onPauseCalled = true;
        _taskLoadVideo.cancel();

        if(StateCasting.instance.isCasting)
            return;

        if(allowBackground)
            StatePlayer.instance.startOrUpdateMediaSession(context, video);
        else {
            when (Settings.instance.playback.backgroundPlay) {
                0 -> handlePause();
                1 -> {
                    if(!(video?.isLive ?: false) && Settings.instance.playback.backgroundSwitchToAudio)
                        _player.switchToAudioMode();
                    StatePlayer.instance.startOrUpdateMediaSession(context, video);
                }
            }
        }
    }
    fun onStop() {
        Logger.i(TAG, "onStop");
        _player.clear();
        StatePlayer.instance.closeMediaSession();
        _overlay_quality_selector?.hide();
        _retryJob?.cancel();
        _retryJob = null;
        _liveTryJob?.cancel();
        _liveTryJob = null;
        _taskLoadVideo.cancel();
        handleStop();
        _didStop = true;
        Logger.i(TAG, "_didStop set to true");

        StatePlayer.instance.rotationLock = false;
        _player.updateRotateLock();
        Logger.i(TAG, "Stopped");
    }
    fun onDestroy() {
        Logger.i(TAG, "onDestroy");
        _destroyed = true;
        _taskLoadVideo.cancel();
        _commentsList.cancel();
        _player.clear();
        _cast.cleanup();
        _container_content_replies.cleanup();
        _container_content_queue.cleanup();
        _container_content_description.cleanup();
        _container_content_support.cleanup();
        StateCasting.instance.onActiveDevicePlayChanged.remove(this);
        StateCasting.instance.onActiveDeviceTimeChanged.remove(this);
        StateCasting.instance.onActiveDeviceConnectionStateChanged.remove(this);
        StateApp.instance.preventPictureInPicture.remove(this);
        StatePlayer.instance.onQueueChanged.remove(this);
        StatePlayer.instance.onVideoChanging.remove(this);
        MediaControlReceiver.onLowerVolumeReceived.remove(this);
        MediaControlReceiver.onPlayReceived.remove(this);
        MediaControlReceiver.onPauseReceived.remove(this);
        MediaControlReceiver.onNextReceived.remove(this);
        MediaControlReceiver.onPreviousReceived.remove(this);
        MediaControlReceiver.onCloseReceived.remove(this);
        MediaControlReceiver.onSeekToReceived.remove(this);

        val job = _jobHideResume;
        _jobHideResume = null;
        job?.cancel();
    }

    //Video Setters
    fun setEmpty() {
        Logger.i(TAG, "setEmpty")

        _title.text = "";
        _rating.visibility = View.GONE;

        _commentsList.clear();
        _minimize_title.text = "";
        _minimize_meta.text = "";
        _platform.clearPlatform();
        _subTitle.text = "";
        _channelName.text = "";
        _channelMeta.text = "";
        _creatorThumbnail.clear();
        setDescription("".fixHtmlWhitespace());
        _descriptionContainer.visibility = View.GONE;
        _player.clear();
        _textComments.visibility = View.INVISIBLE;
        _commentsList.clear();

        _lastVideoSource = null;
        _lastAudioSource = null;
        _lastSubtitleSource = null;
    }
    fun setVideo(url: String, resumeSeconds: Long = 0, playWhenReady: Boolean = true) {
        Logger.i(TAG, "setVideo url=$url resumeSeconds=$resumeSeconds playWhenReady=$playWhenReady")

        if(this.video?.url == url)
            return;

        _searchVideo = null;
        video = null;
        _playbackTracker = null;
        _url = url;
        _videoResumePositionMilliseconds = resumeSeconds * 1000;
        _rating.visibility = View.GONE;
        _layoutRating.visibility = View.GONE;
        _playWhenReady = playWhenReady;
        setLastPositionMilliseconds(_videoResumePositionMilliseconds, false);
        _addCommentView.setContext(null, null);

        _toggleCommentType.setValue(false, false);
        _commentsList.clear();

        setEmpty();

        updateQueueState();

        _retryJob?.cancel();
        _retryJob = null;
        _liveTryJob?.cancel();
        _liveTryJob = null;
        _retryCount = 0;
        fetchVideo();

        switchContentView(_container_content_main);
    }
    fun setVideoOverview(video: IPlatformVideo, fetch: Boolean = true, resumeSeconds: Long = 0, bypassSameVideoCheck: Boolean = false) {
        Logger.i(TAG, "setVideoOverview")

        if(!bypassSameVideoCheck && this.video?.url == video.url)
            return;

        val cachedVideo = StateDownloads.instance.getCachedVideo(video.id);
        if(cachedVideo != null) {
            setVideoDetails(cachedVideo, true);
            return;
        }

        this.video = null;
        this._playbackTracker = null;
        _searchVideo = video;
        _videoResumePositionMilliseconds = resumeSeconds * 1000;
        setLastPositionMilliseconds(_videoResumePositionMilliseconds, false);
        _addCommentView.setContext(null, null);

        _toggleCommentType.setValue(false, false);

        _title.text = getVideoTitle(video, _isAlternativeMetadataShown);
        _rating.visibility = View.GONE;
        _layoutRating.visibility = View.GONE;
        _textComments.visibility = View.VISIBLE;

        _minimize_title.text = getVideoTitle(video, _isAlternativeMetadataShown);
        _minimize_meta.text = video.author.name;

        val subTitleSegments : ArrayList<String> = ArrayList();
        if(video.viewCount > 0)
            subTitleSegments.add("${video.viewCount.toHumanNumber()} ${if(video.isLive)  context.getString(R.string.watching_now) else context.getString(R.string.views)}");
        if(video.datetime != null) {
            val diff = video.datetime?.getNowDiffSeconds() ?: 0;
            val ago = video.datetime?.toHumanNowDiffString(true)
            if(diff >= 0)
                subTitleSegments.add("${ago} ago");
            else
                subTitleSegments.add("available in ${ago}");
        }


        _commentsList.clear();
        _platform.setPlatformFromClientID(video.id.pluginId);
        _subTitle.text = subTitleSegments.joinToString(" • ");
        _playWhenReady = true;
        if(video.author.subscribers != null) {
            _channelMeta.text = if((video.author.subscribers ?: 0) > 0) video.author.subscribers!!.toHumanNumber() + " " + context.getString(R.string.subscribers)else "";
            (_channelName.layoutParams as MarginLayoutParams).setMargins(0, (DP_5 * -1).toInt(), 0, 0);
        } else {
            _channelMeta.text = "";
            (_channelName.layoutParams as MarginLayoutParams).setMargins(0, (DP_2).toInt(), 0, 0);
        }
        setDescription("".fixHtmlWhitespace());
        _player.setMetadata(video.name, video.author.name);

        updateToggleAlternativeMetadataButtonVisibility(video);

        _buttonSubscribe.setSubscribeChannel(video.author.url);

        if(!_description.text.isEmpty())
            _descriptionContainer.visibility = View.VISIBLE;
        else
            _descriptionContainer.visibility = View.GONE;

        _creatorThumbnail.setThumbnail(video.author.thumbnail, false);
        _channelName.text = video.author.name;

        val cachedPolycentricProfile = PolycentricCache.instance.getCachedProfile(video.author.url, true);
        if (cachedPolycentricProfile != null) {
            setPolycentricProfile(cachedPolycentricProfile, animate = false);
            if (cachedPolycentricProfile.expired) {
                _taskLoadPolycentricProfile.run(video.author.id);
            }
        } else {
            setPolycentricProfile(null, animate = false);
            _taskLoadPolycentricProfile.run(video.author.id);
        }

        _player.clear();

        _url = video.url;

        updateQueueState();

        if(fetch) {
            _lastVideoSource = null;
            _lastAudioSource = null;
            _lastSubtitleSource = null;

            _retryJob?.cancel();
            _retryJob = null;
            _liveTryJob?.cancel();
            _liveTryJob = null;
            _retryCount = 0;
            fetchVideo();
        }

        _commentsList.clear();

        switchContentView(_container_content_main);
    }
    //@OptIn(ExperimentalCoroutinesApi::class)
    fun setVideoDetails(videoDetail: IPlatformVideoDetails, newVideo: Boolean = false) {
        Logger.i(TAG, "setVideoDetails (${videoDetail.name})")

        if(newVideo && this.video?.url == videoDetail.url)
            return;

        if (newVideo) {
            _lastVideoSource = null;
            _lastAudioSource = null;
            _lastSubtitleSource = null;
            _isAlternativeMetadataShown = Settings.instance.browsing.showAlternativeMetadataByDefault;
        }

        if(videoDetail.datetime != null && videoDetail.datetime!! > OffsetDateTime.now())
            UIDialogs.toast(context, context.getString(R.string.planned_in) + " ${videoDetail.datetime?.toHumanNowDiffString(true)}")

        if (!videoDetail.isLive) {
            _player.setPlaybackRate(Settings.instance.playback.getDefaultPlaybackSpeed());
        }

        val videoLocal: VideoLocal?;
        val video: IPlatformVideoDetails?;

       if(videoDetail is VideoLocal) {
            videoLocal = videoDetail;
            video = videoDetail;
            this.video = video;
            val videoTask = StatePlatform.instance.getContentDetails(videoDetail.url);
            videoTask.invokeOnCompletion { ex ->
                if(ex != null) {
                    Logger.e(TAG, "Failed to fetch live video for offline video", ex);
                    return@invokeOnCompletion;
                }
                val result = videoTask.getCompleted();
                if(this.video == videoDetail && result is IPlatformVideoDetails) {
                    this.video = result;
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        updateQualitySourcesOverlay(result, videoLocal);
                    }
                }
            };
        }
        else { //TODO: Update cached video if it exists with video
            videoLocal = StateDownloads.instance.getCachedVideo(videoDetail.id);
            video = videoDetail;
        }
        this.videoLocal = videoLocal;
        this.video = video;
        this._playbackTracker = null;

        if(video is JSVideoDetails) {
            val me = this;
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    //TODO: Implement video.getContentChapters()
                    val chapters = null ?: StatePlatform.instance.getContentChapters(video.url);
                    _player.setChapters(chapters);
                    _cast.setChapters(chapters);
                }
                catch(ex: Throwable) {
                    Logger.e(TAG, "Failed to get chapters", ex);
                    _player.setChapters(null);
                    _cast.setChapters(null);

                    /*withContext(Dispatchers.Main) {
                        UIDialogs.toast(context, "Failed to get chapters\n" + ex.message);
                    }*/
                }
                try {
                    val stopwatch = com.futo.platformplayer.debug.Stopwatch()
                    var tracker = video.getPlaybackTracker()
                    Logger.i(TAG, "video.getPlaybackTracker took ${stopwatch.elapsedMs}ms")

                    if (tracker == null) {
                        stopwatch.reset()
                        tracker = StatePlatform.instance.getPlaybackTracker(video.url);
                        Logger.i(TAG, "StatePlatform.instance.getPlaybackTracker took ${stopwatch.elapsedMs}ms")
                    }

                    if(me.video == video)
                        me._playbackTracker = tracker;
                }
                catch(ex: Throwable) {
                    Logger.e(TAG, "Playback tracker failed", ex);
                    if(me.video?.isLive == true) withContext(Dispatchers.Main) {
                        UIDialogs.toast(context, context.getString(R.string.failed_to_get_playback_tracker));
                    };
                    else withContext(Dispatchers.Main) {
                        UIDialogs.showGeneralErrorDialog(context, context.getString(R.string.failed_to_get_playback_tracker), ex);
                    }
                }
            };
        }

        val ref = Models.referenceFromBuffer(video.url.toByteArray())
        val extraBytesRef = video.id.value?.let { if (it.isNotEmpty()) it.toByteArray() else null }
        _addCommentView.setContext(video.url, ref)
        _player.setMetadata(video.name, video.author.name);

        if (video is TutorialFragment.TutorialVideo) {
            _toggleCommentType.setValue(false, false);
        } else {
            _toggleCommentType.setValue(!Settings.instance.other.polycentricEnabled || Settings.instance.comments.defaultCommentSection == 1, false);
        }

        updateCommentType(true);

        //UI
        _title.text = getVideoTitle(video, _isAlternativeMetadataShown);
        _channelName.text = video.author.name;
        if(video.author.subscribers != null) {
            _channelMeta.text = if((video.author.subscribers ?: 0) > 0) video.author.subscribers!!.toHumanNumber() + " " + context.getString(R.string.subscribers) else "";
            (_channelName.layoutParams as MarginLayoutParams).setMargins(0, (DP_5 * -1).toInt(), 0, 0);
        } else {
            _channelMeta.text = "";
            (_channelName.layoutParams as MarginLayoutParams).setMargins(0, (DP_2).toInt(), 0, 0);
        }


        video.author.let {
            if(it is PlatformAuthorMembershipLink && !it.membershipUrl.isNullOrEmpty())
                _monetization.setPlatformMembership(video.id.pluginId, it.membershipUrl);
            else
                _monetization.setPlatformMembership(null, null);
        }

        _minimize_title.text = getVideoTitle(video, _isAlternativeMetadataShown);
        _minimize_meta.text = video.author.name;

        updateToggleAlternativeMetadataButtonVisibility(video);

        _buttonSubscribe.setSubscribeChannel(video.author.url);
        setDescription(video.description.fixHtmlLinks());
        _creatorThumbnail.setThumbnail(video.author.thumbnail, false);

        val cachedPolycentricProfile = PolycentricCache.instance.getCachedProfile(video.author.url, true);
        if (cachedPolycentricProfile != null) {
            setPolycentricProfile(cachedPolycentricProfile, animate = false);
        } else {
            setPolycentricProfile(null, animate = false);
            _taskLoadPolycentricProfile.run(video.author.id);
        }

        _platform.setPlatformFromClientID(video.id.pluginId);
        val subTitleSegments : ArrayList<String> = ArrayList();
        if(video.viewCount > 0)
            subTitleSegments.add("${video.viewCount.toHumanNumber()} ${if(video.isLive) context.getString(R.string.watching_now) else context.getString(R.string.views)}");
        if(video.datetime != null) {
            val diff = video.datetime?.getNowDiffSeconds() ?: 0;
            val ago = video.datetime?.toHumanNowDiffString(true)
            if(diff >= 0)
                subTitleSegments.add("${ago} ago");
            else
                subTitleSegments.add("available in ${ago}");
        }
        _subTitle.text = subTitleSegments.joinToString(" • ");

        _rating.onLikeDislikeUpdated.remove(this);

        _rating.visibility = View.GONE;

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val queryReferencesResponse = ApiMethods.getQueryReferences(PolycentricCache.SERVER, ref, null,null,
                    arrayListOf(
                        Protocol.QueryReferencesRequestCountLWWElementReferences.newBuilder().setFromType(ContentType.OPINION.value).setValue(
                            ByteString.copyFrom(Opinion.like.data)).build(),
                        Protocol.QueryReferencesRequestCountLWWElementReferences.newBuilder().setFromType(ContentType.OPINION.value).setValue(
                            ByteString.copyFrom(Opinion.dislike.data)).build()
                    ),
                    extraByteReferences = listOfNotNull(extraBytesRef)
                );

                val likes = queryReferencesResponse.countsList[0];
                val dislikes = queryReferencesResponse.countsList[1];
                val hasLiked = StatePolycentric.instance.hasLiked(ref.toByteArray())/* || extraBytesRef?.let { StatePolycentric.instance.hasLiked(it) } ?: false*/;
                val hasDisliked = StatePolycentric.instance.hasDisliked(ref.toByteArray())/* || extraBytesRef?.let { StatePolycentric.instance.hasDisliked(it) } ?: false*/;

                withContext(Dispatchers.Main) {
                    _rating.visibility = View.VISIBLE;
                    _rating.setRating(RatingLikeDislikes(likes, dislikes), hasLiked, hasDisliked);
                    _rating.onLikeDislikeUpdated.subscribe(this) { args ->
                        if (args.hasLiked) {
                            args.processHandle.opinion(ref, Opinion.like);
                        } else if (args.hasDisliked) {
                            args.processHandle.opinion(ref, Opinion.dislike);
                        } else {
                            args.processHandle.opinion(ref, Opinion.neutral);
                        }

                        fragment.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                Logger.i(TAG, "Started backfill");
                                args.processHandle.fullyBackfillServersAnnounceExceptions();
                                Logger.i(TAG, "Finished backfill");
                            } catch (e: Throwable) {
                                Logger.e(TAG, "Failed to backfill servers", e)
                            }
                        }

                        StatePolycentric.instance.updateLikeMap(ref, args.hasLiked, args.hasDisliked)
                    };
                }
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to get polycentric likes/dislikes.", e);
                _rating.visibility = View.GONE;
            }
        }

        when (video.rating) {
            is RatingLikeDislikes -> {
                val r = video.rating as RatingLikeDislikes;
                _layoutRating.visibility = View.VISIBLE;

                _textLikes.visibility = View.VISIBLE;
                _imageLikeIcon.visibility = View.VISIBLE;
                _textLikes.text = r.likes.toHumanNumber();

                _imageDislikeIcon.visibility = View.VISIBLE;
                _textDislikes.visibility = View.VISIBLE;
                _textDislikes.text = r.dislikes.toHumanNumber();
            }
            is RatingLikes -> {
                val r = video.rating as RatingLikes;
                _layoutRating.visibility = View.VISIBLE;

                _textLikes.visibility = View.VISIBLE;
                _imageLikeIcon.visibility = View.VISIBLE;
                _textLikes.text = r.likes.toHumanNumber();

                _imageDislikeIcon.visibility = View.GONE;
                _textDislikes.visibility = View.GONE;
            }
            else -> {
                _layoutRating.visibility = View.GONE;
            }
        }


        //Overlay
        updateQualitySourcesOverlay(video, videoLocal);

        setLoading(false);

        //Set Mediasource

        val toResume = _videoResumePositionMilliseconds;
        _videoResumePositionMilliseconds = 0;
        loadCurrentVideo(toResume);
        if (!Settings.instance.gestureControls.useSystemVolume) {
            _player.setGestureSoundFactor(1.0f);
        }

        updateQueueState();

        if (video !is TutorialFragment.TutorialVideo) {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val historyItem = getHistoryIndex(videoDetail) ?: return@launch;

                withContext(Dispatchers.Main) {
                    _historicalPosition = StateHistory.instance.updateHistoryPosition(video,  historyItem,false, (toResume.toFloat() / 1000.0f).toLong());
                    Logger.i(TAG, "Historical position: $_historicalPosition, last position: $lastPositionMilliseconds");
                    if (_historicalPosition > 60 && video.duration - _historicalPosition > 5 && Math.abs(_historicalPosition - lastPositionMilliseconds / 1000) > 5.0) {
                        _layoutResume.visibility = View.VISIBLE;
                        _textResume.text = "Resume at ${_historicalPosition.toHumanTime(false)}";

                        _jobHideResume = fragment.lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                delay(8000);
                                _layoutResume.visibility = View.GONE;
                                _textResume.text = "";
                            } catch (e: Throwable) {
                                Logger.e(TAG, "Failed to set resume changes.", e);
                            }
                        }
                    } else {
                        _layoutResume.visibility = View.GONE;
                        _textResume.text = "";
                    }
                }
            }
        }

        StatePlayer.instance.startOrUpdateMediaSession(context, video);
        StatePlayer.instance.setCurrentlyPlaying(video);

        if(video.isLive && video.live != null) {
            loadLiveChat(video);
        }
        if(video.isLive && video.live == null && !video.video.videoSources.any())
            startLiveTry(video);

        _player.updateNextPrevious();
        updateMoreButtons();

        if (videoDetail is TutorialFragment.TutorialVideo) {
            _buttonSubscribe.visibility = View.GONE
            _buttonMore.visibility = View.GONE
            _buttonPins.visibility = View.GONE
            _layoutRating.visibility = View.GONE
            _layoutToggleCommentSection.visibility = View.GONE
        } else {
            _buttonSubscribe.visibility = View.VISIBLE
            _buttonMore.visibility = View.VISIBLE
            _buttonPins.visibility = View.VISIBLE
            _layoutRating.visibility = View.VISIBLE
            _layoutToggleCommentSection.visibility = View.VISIBLE
        }
    }
    fun loadLiveChat(video: IPlatformVideoDetails) {
        _liveChat?.stop();
        _container_content_liveChat.cancel();
        _liveChat = null;

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                var livePager: IPager<IPlatformLiveEvent>?;
                var liveChatWindow: ILiveChatWindowDescriptor?;
                try {
                    //TODO: Create video.getLiveEvents shortcut/optimalization
                    livePager = StatePlatform.instance.getLiveEvents(video.url);
                } catch (ex: Throwable) {
                    livePager = null;
                    UIDialogs.toast(context.getString(R.string.exception_retrieving_live_events) + "\n" + ex.message);
                    Logger.e(TAG, "Failed to retrieve live chat events", ex);
                }
                try {
                    //TODO: Create video.getLiveChatWindow shortcut/optimalization
                    liveChatWindow = if(Settings.instance.playback.useLiveChatWindow)
                        StatePlatform.instance.getLiveChatWindow(video.url);
                    else null;
                }
                catch(ex: Throwable) {
                    liveChatWindow = null;
                    UIDialogs.toast(context.getString(R.string.exception_retrieving_live_chat_window) + "\n" + ex.message);
                    Logger.e(TAG, "Failed to retrieve live chat window", ex);
                }
                val liveChat = livePager?.let {
                    val liveChatManager = LiveChatManager(fragment.lifecycleScope, livePager, video.viewCount);
                    liveChatManager.start();
                    return@let liveChatManager;
                }
                _liveChat = liveChat;

                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        _container_content_liveChat.load(fragment.lifecycleScope, liveChat, liveChatWindow, if(liveChat != null) video.viewCount else null);
                        switchContentView(_container_content_liveChat);
                    } catch (e: Throwable) {
                        Logger.e(TAG, "Failed to switch content view to live chat.");
                    }
                }
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Failed to load live chat", ex);

                UIDialogs.toast(context.getString(R.string.live_chat_failed_to_load) + "\n" + ex.message);
                //_liveChat?.handleEvents(listOf(LiveEventComment("SYSTEM", null, "Failed to load live chat:\n" + ex.message, "#FF0000")))
                /*
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    UIDialogs.showGeneralRetryErrorDialog(context, "Failed to load live chat", ex, { loadLiveChat(video); });
                } */
            }
        }
    }

    //Source Loads
    private fun loadCurrentVideo(resumePositionMs: Long = 0) {
        _didStop = false;

        val video = (videoLocal ?: video) ?: return;

        try {
            val videoSource = _lastVideoSource ?: _player.getPreferredVideoSource(video, Settings.instance.playback.getCurrentPreferredQualityPixelCount());
            val audioSource = _lastAudioSource ?: _player.getPreferredAudioSource(video, Settings.instance.playback.getPrimaryLanguage(context));
            val subtitleSource = _lastSubtitleSource ?: (if(video is VideoLocal) video.subtitlesSources.firstOrNull() else null);
            Logger.i(TAG, "loadCurrentVideo(videoSource=$videoSource, audioSource=$audioSource, subtitleSource=$subtitleSource, resumePositionMs=$resumePositionMs)")

            if(videoSource == null && audioSource == null) {
                handleUnavailableVideo();
                StatePlatform.instance.clearContentDetailCache(video.url);
                return;
            }

            val isCasting = StateCasting.instance.isCasting
            if (!isCasting) {
                setCastEnabled(false);

                val thumbnail = video.thumbnails.getHQThumbnail();
                if (videoSource == null && !thumbnail.isNullOrBlank())
                    Glide.with(context).asBitmap().load(thumbnail)
                        .into(object: CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                _player.setArtwork(BitmapDrawable(resources, resource));
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                                _player.setArtwork(null);
                            }
                        });
                else
                    _player.setArtwork(null);

                _player.setSource(videoSource, audioSource, _playWhenReady, false);
                if(subtitleSource != null)
                    _player.swapSubtitles(fragment.lifecycleScope, subtitleSource);
                _player.seekTo(resumePositionMs);
            }
            else
                loadCurrentVideoCast(video, videoSource, audioSource, subtitleSource, resumePositionMs, Settings.instance.playback.getDefaultPlaybackSpeed().toDouble());

            _lastVideoSource = videoSource;
            _lastAudioSource = audioSource;
            _lastSubtitleSource = subtitleSource;
        }
        catch(ex: UnsupportedCastException) {
            Logger.e(TAG, "Failed to load cast media", ex);
            UIDialogs.showGeneralErrorDialog(context, context.getString(R.string.unsupported_cast_format), ex);
        }
        catch(ex: Throwable) {
            Logger.e(TAG, "Failed to load media", ex);
            UIDialogs.showGeneralErrorDialog(context, context.getString(R.string.failed_to_load_media), ex);
        }
    }
    private fun loadCurrentVideoCast(video: IPlatformVideoDetails, videoSource: IVideoSource?, audioSource: IAudioSource?, subtitleSource: ISubtitleSource?, resumePositionMs: Long, speed: Double?) {
        Logger.i(TAG, "loadCurrentVideoCast(video=$video, videoSource=$videoSource, audioSource=$audioSource, resumePositionMs=$resumePositionMs)")

        if(StateCasting.instance.castIfAvailable(context.contentResolver, video, videoSource, audioSource, subtitleSource, resumePositionMs, speed)) {
            _cast.setVideoDetails(video, resumePositionMs / 1000);
            setCastEnabled(true);
        } else throw IllegalStateException("Disconnected cast during loading");
    }

    //Events
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun onSourceChanged(videoSource: IVideoSource?, audioSource: IAudioSource?, resume: Boolean){
        Logger.i(TAG, "onSourceChanged(videoSource=$videoSource, audioSource=$audioSource, resume=$resume)")

        if((videoSource == null || videoSource is LocalVideoSource) && (audioSource == null || audioSource is LocalAudioSource))
            UIDialogs.toast(context, context.getString(R.string.offline_playback), false);
        //If LiveStream, set to end
        if(videoSource is IDashManifestSource || videoSource is IHLSManifestSource) {
            if (video?.isLive == true) {
                _player.seekToEnd(6000);
            }

            val videoTracks = _player.exoPlayer?.player?.currentTracks?.groups?.firstOrNull { it.mediaTrackGroup.type == C.TRACK_TYPE_VIDEO }
            val audioTracks = _player.exoPlayer?.player?.currentTracks?.groups?.firstOrNull { it.mediaTrackGroup.type == C.TRACK_TYPE_AUDIO }

            val videoTrackFormats = mutableListOf<Format>();
            val audioTrackFormats = mutableListOf<Format>();

            if(videoTracks != null) {
                for (i in 0 until videoTracks.mediaTrackGroup.length)
                    videoTrackFormats.add(videoTracks.mediaTrackGroup.getFormat(i));
            }
            if(audioTracks != null) {
                for (i in 0 until audioTracks.mediaTrackGroup.length)
                    audioTrackFormats.add(audioTracks.mediaTrackGroup.getFormat(i));
            }

            updateQualityFormatsOverlay(
                videoTrackFormats.distinctBy { it.height }.sortedBy { it.height },
                audioTrackFormats.distinctBy { it.bitrate }.sortedBy { it.bitrate });
        }
    }

    private var _didTriggerDatasourceError = false;
    private fun onDataSourceError(exception: Throwable) {
        Logger.e(TAG, "onDataSourceError", exception);
        if(exception.cause != null && exception.cause is HttpDataSource.InvalidResponseCodeException && (exception.cause!! as HttpDataSource.InvalidResponseCodeException).responseCode == 403) {
            val currentVideo = video
            if(currentVideo == null || currentVideo !is IPluginSourced)
                return;
            val config = currentVideo.sourceConfig;

            if(!_didTriggerDatasourceError) {
                _didTriggerDatasourceError = true;

                UIDialogs.showDialog(context, R.drawable.ic_error_pred,
                    context.getString(R.string.media_error),
                    context.getString(R.string.the_media_source_encountered_an_unauthorized_error_this_might_be_solved_by_a_plugin_reload_would_you_like_to_reload_experimental),
                    null,
                    0,
                        UIDialogs.Action(context.getString(R.string.no), { _didTriggerDatasourceError = false }),
                        UIDialogs.Action(context.getString(R.string.yes), {
                            fragment.lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    StatePlatform.instance.reloadClient(context, config.id);
                                    reloadVideo();
                                } catch (e: Throwable) {
                                    Logger.e(TAG, "Failed to reload video.", e)
                                }
                            }
                        }, UIDialogs.ActionStyle.PRIMARY)
                    );
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.actionMasked == MotionEvent.ACTION_CANCEL ||
            ev?.actionMasked == MotionEvent.ACTION_POINTER_DOWN ||
            ev?.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            onTouchCancel.emit();
        }

        return super.onInterceptTouchEvent(ev);
    }


    //Actions
    private fun showVideoSettings() {
        Logger.i(TAG, "showVideoSettings")
        _overlay_quality_selector?.selectOption("video", _lastVideoSource);
        _overlay_quality_selector?.selectOption("audio", _lastAudioSource);
        _overlay_quality_selector?.selectOption("subtitles", _lastSubtitleSource);
        val currentPlaybackRate = (if (_isCasting) StateCasting.instance.activeDevice?.speed else _player.getPlaybackRate()) ?: 1.0
        _overlay_quality_selector?.groupItems?.firstOrNull { it is SlideUpMenuButtonList && it.id == "playback_rate" }?.let {
            (it as SlideUpMenuButtonList).setSelected(currentPlaybackRate.toString())
        };

        _overlay_quality_selector?.show();
        _slideUpOverlay = _overlay_quality_selector;
    }


    fun prevVideo(withoutRemoval: Boolean = false) {
        Logger.i(TAG, "prevVideo")
        val next = StatePlayer.instance.prevQueueItem(withoutRemoval || _player.duration < 100 || (_player.position.toFloat() / _player.duration) < 0.9);
        if(next != null) {
            setVideoOverview(next, true, 0, true);
        }
    }

    fun nextVideo(forceLoop: Boolean = false, withoutRemoval: Boolean = false, bypassVideoLoop: Boolean = false): Boolean {
        Logger.i(TAG, "nextVideo")
        var next = StatePlayer.instance.nextQueueItem(withoutRemoval || _player.duration < 100 || (_player.position.toFloat() / _player.duration) < 0.9, bypassVideoLoop);
        if(next == null && forceLoop)
            next = StatePlayer.instance.restartQueue();
        if(next != null) {
            setVideoOverview(next, true, 0, true);
            return true;
        }
        else
            StatePlayer.instance.setCurrentlyPlaying(null);
        return false;
    }

    //Quality Selector data
    private fun updateQualityFormatsOverlay(liveStreamVideoFormats : List<Format>?, liveStreamAudioFormats : List<Format>?) {
        val v = video ?: return;
        updateQualitySourcesOverlay(v, videoLocal, liveStreamVideoFormats, liveStreamAudioFormats);
    }
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun updateQualitySourcesOverlay(videoDetails: IPlatformVideoDetails?, videoLocal: VideoLocal? = null, liveStreamVideoFormats: List<Format>? = null, liveStreamAudioFormats: List<Format>? = null) {
        Logger.i(TAG, "updateQualitySourcesOverlay");

        val video: IPlatformVideoDetails?;
        val localVideoSources: List<LocalVideoSource>?;
        val localAudioSource: List<LocalAudioSource>?;
        val localSubtitleSources: List<LocalSubtitleSource>?;

        val videoSources: List<IVideoSource>?;
        val audioSources: List<IAudioSource>?;

        if(videoDetails is VideoLocal) {
            video = videoLocal?.videoSerialized;
            localVideoSources = videoDetails.videoSource.toList();
            localAudioSource = videoDetails.audioSource.toList();
            localSubtitleSources = videoDetails.subtitlesSources.toList();
            videoSources = null
            audioSources = null;
        }
        else {
            video = videoDetails;
            videoSources = video?.video?.videoSources?.toList();
            audioSources = if(video?.video?.isUnMuxed == true)
                (video.video as VideoUnMuxedSourceDescriptor).audioSources.toList()
            else null
            if(videoLocal != null) {
                localVideoSources = videoLocal.videoSource.toList();
                localAudioSource = videoLocal.audioSource.toList();
                localSubtitleSources = videoLocal.subtitlesSources.toList();
            }
            else {
                localVideoSources = null;
                localAudioSource = null;
                localSubtitleSources = null;
            }
        }

        val bestVideoSources = videoSources?.map { it.height * it.width }
            ?.distinct()
            ?.map { x -> VideoHelper.selectBestVideoSource(videoSources.filter { x == it.height * it.width }, -1, FutoVideoPlayerBase.PREFERED_VIDEO_CONTAINERS) }
            ?.filter { it != null }
            ?.toList() ?: listOf();
        val bestAudioContainer = audioSources?.let { VideoHelper.selectBestAudioSource(it, FutoVideoPlayerBase.PREFERED_AUDIO_CONTAINERS)?.container };
        val bestAudioSources = audioSources
            ?.filter { it.container == bestAudioContainer }
            ?.toList() ?: listOf();

        val canSetSpeed = !_isCasting || StateCasting.instance.activeDevice?.canSetSpeed == true
        val currentPlaybackRate = if (_isCasting) StateCasting.instance.activeDevice?.speed else _player.getPlaybackRate()
        _overlay_quality_selector = SlideUpMenuOverlay(this.context, _overlay_quality_container, context.getString(
                    R.string.quality), null, true,
            if (canSetSpeed) SlideUpMenuTitle(this.context).apply { setTitle(context.getString(R.string.playback_rate)) } else null,
            if (canSetSpeed) SlideUpMenuButtonList(this.context, null, "playback_rate").apply {
                setButtons(listOf("0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0", "2.25"), currentPlaybackRate!!.toString());
                onClick.subscribe { v ->
                    if (_isCasting) {
                        val ad = StateCasting.instance.activeDevice ?: return@subscribe
                        if (!ad.canSetSpeed) {
                            return@subscribe
                        }

                        ad.changeSpeed(v.toDouble())
                        setSelected(v);
                    } else {
                        _player.setPlaybackRate(v.toFloat());
                        setSelected(v);
                    }
                };
            } else null,

            if(localVideoSources?.isNotEmpty() == true)
                SlideUpMenuGroup(this.context, context.getString(R.string.offline_video), "video",
                    *localVideoSources
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_movie, it.name, "${it.width}x${it.height}", it,
                                { handleSelectVideoTrack(it) });
                        }.toList().toTypedArray())
            else null,
            if(localAudioSource?.isNotEmpty() == true)
                SlideUpMenuGroup(this.context, context.getString(R.string.offline_audio), "audio",
                    *localAudioSource
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_music, it.name, it.bitrate.toHumanBitrate(), it,
                                { handleSelectAudioTrack(it) });
                        }.toList().toTypedArray())
            else null,
            if(localSubtitleSources?.isNotEmpty() == true)
                SlideUpMenuGroup(this.context, context.getString(R.string.offline_subtitles), "subtitles",
                    *localSubtitleSources
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_edit, it.name, "", it,
                                { handleSelectSubtitleTrack(it) })
                        }.toList().toTypedArray())
            else null,
            if(liveStreamVideoFormats?.isEmpty() == false)
                SlideUpMenuGroup(this.context, context.getString(R.string.stream_video), "video",
                    *liveStreamVideoFormats
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_movie, it.label ?: it.containerMimeType ?: it.bitrate.toString(), "${it.width}x${it.height}", it,
                                { _player.selectVideoTrack(it.height) });
                        }.toList().toTypedArray())
            else null,
            if(liveStreamAudioFormats?.isEmpty() == false)
                SlideUpMenuGroup(this.context, context.getString(R.string.stream_audio), "audio",
                    *liveStreamAudioFormats
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_music, "${it.label ?: it.containerMimeType} ${it.bitrate}", "", it,
                                { _player.selectAudioTrack(it.bitrate) });
                        }.toList().toTypedArray())
            else null,

            if(bestVideoSources.isNotEmpty())
                SlideUpMenuGroup(this.context, context.getString(R.string.video), "video",
                    *bestVideoSources
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_movie, it!!.name, if (it.width > 0 && it.height > 0) "${it.width}x${it.height}" else "", it,
                                { handleSelectVideoTrack(it) });
                        }.toList().toTypedArray())
            else null,
            if(bestAudioSources.isNotEmpty())
                SlideUpMenuGroup(this.context, context.getString(R.string.audio), "audio",
                    *bestAudioSources
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_music, it.name, it.bitrate.toHumanBitrate(), it,
                                { handleSelectAudioTrack(it) });
                        }.toList().toTypedArray())
            else null,
            if(video?.subtitles?.isNotEmpty() == true)
                SlideUpMenuGroup(this.context, context.getString(R.string.subtitles), "subtitles",
                    *video.subtitles
                        .map {
                            SlideUpMenuItem(this.context, R.drawable.ic_edit, it.name, "", it,
                                { handleSelectSubtitleTrack(it) })
                        }.toList().toTypedArray())
            else null);
    }

    private fun updateQueueState() {
        _upNext.update();
        /*_player.updateNextPrevious(
            getPreviousVideo(withoutRemoval = true, forceLoop = true) != null,
            getNextVideo(withoutRemoval = true, forceLoop = true) != null
        )*/
    }

    //Handlers
    private fun handlePlay() {
        Logger.i(TAG, "handlePlay")
        if (!StateCasting.instance.resumeVideo()) {
            _player.play();
        }

        //TODO: This was needed because handleLowerVolume was done.
        //_player.setVolume(1.0f);
    }

    private fun handleLowerVolume() {
        Logger.i(TAG, "handleLowerVolume")
        //TODO: This seems to be handled by OS?
        //_player.setVolume(0.2f);
    }

    private fun handlePause() {
        Logger.i(TAG, "handlePause")
        if (!StateCasting.instance.pauseVideo()) {
            _player.pause();
        }
    }
    private fun handleSeek(ms: Long) {
        Logger.i(TAG, "handleSeek(ms=$ms)")
        if (!StateCasting.instance.videoSeekTo(ms.toDouble() / 1000.0)) {
            _player.seekTo(ms);
        }
    }
    private fun handleStop() {
        Logger.i(TAG, "handleStop")
        if (!StateCasting.instance.stopVideo()) {
            _player.stop();
        }
    }

    private fun handlePlayChanged(playing: Boolean) {
        Logger.i(TAG, "handlePlayChanged(playing=$playing)")

        val ad = StateCasting.instance.activeDevice;
        if (ad != null) {
            _cast.setIsPlaying(playing);
        } else {
            StatePlayer.instance.updateMediaSession( null);
            StatePlayer.instance.updateMediaSessionPlaybackState(_player.exoPlayer?.getPlaybackStateCompat() ?: PlaybackStateCompat.STATE_NONE, _player.exoPlayer?.player?.currentPosition ?: 0);
        }

        if(playing) {
            _minimize_controls_pause.visibility = View.VISIBLE;
            _minimize_controls_play.visibility = View.GONE;

            if (_isCasting) {
                if (Settings.instance.casting.keepScreenOn) {
                    Logger.i(TAG, "Keep screen on set handlePlayChanged casting")
                    fragment.activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } else {
                Logger.i(TAG, "Keep screen on set handlePlayChanged player")
                fragment.activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        else {
            _minimize_controls_pause.visibility = View.GONE;
            _minimize_controls_play.visibility = View.VISIBLE;

            Logger.i(TAG, "Keep screen on unset handlePlayChanged")
            fragment.activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        isPlaying = playing;
        onPlayChanged.emit(playing);
        updateTracker(lastPositionMilliseconds, playing, true);
    }

    private fun handleSelectVideoTrack(videoSource: IVideoSource) {
        Logger.i(TAG, "handleSelectAudioTrack(videoSource=$videoSource)")
        val video = video ?: return;

        if(_lastVideoSource == videoSource)
            return;

        val d = StateCasting.instance.activeDevice;
        if (d != null && d.connectionState == CastConnectionState.CONNECTED)
            StateCasting.instance.castIfAvailable(context.contentResolver, video, videoSource, _lastAudioSource, _lastSubtitleSource, (d.expectedCurrentTime * 1000.0).toLong(), d.speed);
        else if(!_player.swapSources(videoSource, _lastAudioSource, true, true, true))
            _player.hideControls(false); //TODO: Disable player?

        _lastVideoSource = videoSource;
    }
    private fun handleSelectAudioTrack(audioSource: IAudioSource) {
        Logger.i(TAG, "handleSelectAudioTrack(audioSource=$audioSource)")
        val video = video ?: return;

        if(_lastAudioSource == audioSource)
            return;

        val d = StateCasting.instance.activeDevice;
        if (d != null && d.connectionState == CastConnectionState.CONNECTED)
            StateCasting.instance.castIfAvailable(context.contentResolver, video, _lastVideoSource, audioSource, _lastSubtitleSource, (d.expectedCurrentTime * 1000.0).toLong(), d.speed);
        else(!_player.swapSources(_lastVideoSource, audioSource, true, true, true))
        _player.hideControls(false); //TODO: Disable player?

        _lastAudioSource = audioSource;
    }
    private fun handleSelectSubtitleTrack(subtitleSource: ISubtitleSource) {
        Logger.i(TAG, "handleSelectSubtitleTrack(subtitleSource=$subtitleSource)")
        val video = video ?: return;

        var toSet: ISubtitleSource? = subtitleSource
        if(_lastSubtitleSource == subtitleSource)
            toSet = null;

        val d = StateCasting.instance.activeDevice;
        if (d != null && d.connectionState == CastConnectionState.CONNECTED)
            StateCasting.instance.castIfAvailable(context.contentResolver, video, _lastVideoSource, _lastAudioSource, toSet, (d.expectedCurrentTime * 1000.0).toLong(), d.speed);
        else
            _player.swapSubtitles(fragment.lifecycleScope, toSet);

        _lastSubtitleSource = toSet;
    }

    private fun handleUnavailableVideo(msg: String? = null) {
        if (!nextVideo()) {
            if(video?.datetime == null || video?.datetime!! < OffsetDateTime.now().minusHours(1))
                UIDialogs.showDialog(context, R.drawable.ic_lock, context.getString(R.string.unavailable_video), msg ?: context.getString(R.string.this_video_is_unavailable), null, 0,
                    UIDialogs.Action(context.getString(R.string.back), {
                        this@VideoDetailView.onClose.emit();
                    }, UIDialogs.ActionStyle.PRIMARY)
                );
        } else {
            StateAnnouncement.instance.registerAnnouncement(video?.id?.value + "_Q_UNAVAILABLE", context.getString(R.string.unavailable_video), context.getString(R.string.there_was_an_unavailable_video_in_your_queue_videoname_by_authorname).replace("{videoName}", video?.name ?: "").replace("{authorName}", video?.author?.name ?: ""), AnnouncementType.SESSION)
        }

        video?.let { StatePlatform.instance.clearContentDetailCache(it.url) };
    }


    //Fetch
    private fun fetchComments() {
        Logger.i(TAG, "fetchComments")
        video?.let {
            _commentsList.load(true) { StatePlatform.instance.getComments(it); };
        }
    }
    private fun fetchPolycentricComments() {
        Logger.i(TAG, "fetchPolycentricComments")
        val video = video;
        val idValue = video?.id?.value
        if (video?.url?.isEmpty() != false) {
            Logger.w(TAG, "Failed to fetch polycentric comments because url was null")
            _commentsList.clear()
            return
        }

        val ref = Models.referenceFromBuffer(video.url.toByteArray())
        val extraBytesRef = idValue?.let { if (it.isNotEmpty()) it.toByteArray() else null }
        _commentsList.load(false) { StatePolycentric.instance.getCommentPager(video.url, ref, listOfNotNull(extraBytesRef)); };
    }
    private fun fetchVideo() {
        Logger.i(TAG, "fetchVideo")
        video = null;
        _playbackTracker = null;

        val url = _url;
        if (url != null && url.isNotBlank()) {
            setLoading(true);
            _taskLoadVideo.run(url);
        }
    }

    private fun handleFullScreen(fullscreen : Boolean) {
        Logger.i(TAG, "handleFullScreen(fullscreen=$fullscreen)")

        if(fullscreen) {
            _layoutPlayerContainer.setPadding(0, 0, 0, 0);

            val lp = _container_content.layoutParams as ConstraintLayout.LayoutParams;
            lp.topMargin = 0;
            _container_content.layoutParams = lp;

            this._player.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            setProgressBarOverlayed(null);
        }
        else {
            _layoutPlayerContainer.setPadding(0, 0, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6.0f, Resources.getSystem().displayMetrics).toInt());

            val lp = _container_content.layoutParams as ConstraintLayout.LayoutParams;
            lp.topMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -18.0f, Resources.getSystem().displayMetrics).toInt();
            _container_content.layoutParams = lp;

            this._player.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setProgressBarOverlayed(false);
        }
        onFullscreenChanged.emit(fullscreen);
    }

    private fun setCastEnabled(isCasting: Boolean) {
        Logger.i(TAG, "setCastEnabled(isCasting=$isCasting)")

        video?.let { updateQualitySourcesOverlay(it, videoLocal); };

        val changed = _isCasting != isCasting;
        _isCasting = isCasting;

        if(isCasting) {
            setFullscreen(false);
            _player.stop();
            _player.hideControls(false);
            _cast.visibility = View.VISIBLE;
        } else {
            StateCasting.instance.stopVideo();
            _cast.stopTimeJob();
            _cast.visibility = View.GONE;

            if (video?.isLive == false) {
                _player.setPlaybackRate(Settings.instance.playback.getDefaultPlaybackSpeed());
            }
        }

        if (changed) {
            stopAllGestures();
        }
    }

    fun setFullscreen(fullscreen : Boolean) {
        Logger.i(TAG, "setFullscreen(fullscreen=$fullscreen)")
        _player.setFullScreen(fullscreen);
    }
    private fun setLoading(isLoading : Boolean) {
        if(isLoading){
            (_overlay_loading_spinner.drawable as Animatable?)?.start()
            _overlay_loading.visibility = View.VISIBLE;
        }
        else {
            _overlay_loading.visibility = View.GONE;
            (_overlay_loading_spinner.drawable as Animatable?)?.stop()
        }
    }

    //UI Actions
    private fun toggleAlternativeMetadata(view: View) {
        _isAlternativeMetadataShown = !_isAlternativeMetadataShown;

        _title.text = getVideoTitle(video as IPlatformVideo, _isAlternativeMetadataShown);
        _minimize_title.text = getVideoTitle(video as IPlatformVideo, _isAlternativeMetadataShown);

        setAlternativeMetadataButtonState(_buttonToggleAlternativeMetadata, _isAlternativeMetadataShown);
    }

    private fun getVideoTitle(video: IPlatformVideo, alternative: Boolean): String {
        return if (alternative) (video.alternativeName ?: video.name) else video.name;
    }

    private fun updateToggleAlternativeMetadataButtonVisibility(video: IPlatformVideo) {
        if (video.alternativeName != null || video.thumbnails.hasAlternative()) {
            _buttonToggleAlternativeMetadata.visibility = VISIBLE;
            _buttonToggleAlternativeMetadataMinimized.visibility = VISIBLE;
            setAlternativeMetadataButtonState(_buttonToggleAlternativeMetadata, _isAlternativeMetadataShown);
        } else {
            _buttonToggleAlternativeMetadata.visibility = GONE;
            _buttonToggleAlternativeMetadataMinimized.visibility = GONE;
        }
    }

    private fun setAlternativeMetadataButtonState(button: ImageButton, active: Boolean) {
        // Simulate grayscale effect from YouTube DeArrow extension
        val colorMatrix = ColorMatrix();
        if (!active) {
            colorMatrix.setSaturation(0F);
        }
        button.colorFilter = ColorMatrixColorFilter(colorMatrix);
    }

    private fun setDescription(text: Spanned) {
        _container_content_description.load(text);
        _description.text = text;

        if (_description.text.isNotEmpty())
            _descriptionContainer.visibility = View.VISIBLE;
        else
            _descriptionContainer.visibility = View.GONE;
    }
    fun onBackPressed(): Boolean {
        val slideUpOverlay = _slideUpOverlay;
        if (slideUpOverlay != null) {
            if (slideUpOverlay.isVisible) {
                slideUpOverlay.hide();
                return true;
            } else {
                _slideUpOverlay = null;
            }
        }

        if (_container_content_current != _container_content_main) {
            switchContentView(_container_content_main);
            return true;
        }

        return false;
    }
    private fun switchContentView(view: View) {
        val curView = _container_content_current;
        if (curView == view)
            return;

        val animHeight = _container_content.height;


        if(view == _container_content_main) {
            curView.elevation = 2f;
            view.elevation = 1f;
            view.visibility = VISIBLE;

            curView.animate()
                .setDuration(300)
                .translationY(animHeight.toFloat())
                .withEndAction {
                    curView.visibility = GONE;
                    _container_content_current = view;
                }
                .start();
        }
        else {
            curView.elevation = 1f;
            view.elevation = 2f;
            view.translationY = animHeight.toFloat();
            view.visibility = VISIBLE;

            view.animate()
                .setDuration(300)
                .translationY(0f)
                .withEndAction {
                    curView.visibility = GONE;
                    _container_content_current = view;
                }
                .start();
        }
    }

    //TODO: Make pill buttons dynamic instead of visiblity
    private fun updatePillButtonVisibilities() {
        _buttonPins.setButtonVisibility {
            (it.tagRef != TAG_BACKGROUND && it.tagRef != TAG_OVERLAY) || !_isCasting
        };
    }

    private fun updateCommentType(reloadComments: Boolean) {
        if (_toggleCommentType.value) {
            _textCommentType.text = "Platform";
            _addCommentView.visibility = View.GONE;

            if (reloadComments) {
                fetchComments();
            }
        } else {
            _textCommentType.text = "Polycentric";
            _addCommentView.visibility = View.VISIBLE;

            if (reloadComments) {
                fetchPolycentricComments()
            }
        }
    }


    //Picture2Picture
    fun startPictureInPicture() {
        Logger.i(TAG, "startPictureInPicture")

        UIDialogs.dismissAllDialogs();
        onMaximize.emit(true);
        onEnterPictureInPicture.emit();
        _player.hideControls(false);
        _layoutResume.visibility = View.GONE;
    }
    fun handleEnterPictureInPicture() {
        Logger.i(TAG, "handleEnterPictureInPicture");

        _overlayContainer.removeAllViews();
        _overlay_quality_selector?.hide();

        _player.fillHeight();
        _layoutPlayerContainer.setPadding(0, 0, 0, 0);
    }
    fun handleLeavePictureInPicture() {
        Logger.i(TAG, "handleLeavePictureInPicture")

        if(!_player.isFullScreen) {
            _player.fitHeight();
            _layoutPlayerContainer.setPadding(0, 0, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6.0f, Resources.getSystem().displayMetrics).toInt());
        } else {
            _layoutPlayerContainer.setPadding(0, 0, 0, 0);
        }
    }
    fun getPictureInPictureParams() : PictureInPictureParams {
        var videoSourceWidth = _player.exoPlayer?.player?.videoSize?.width ?: 0;
        var videoSourceHeight = _player.exoPlayer?.player?.videoSize?.height ?: 0;

        if(videoSourceWidth == 0 || videoSourceHeight == 0) {
            videoSourceWidth = 16;
            videoSourceHeight = 9;
        }
        val aspectRatio = videoSourceWidth.toDouble() / videoSourceHeight;
        if(aspectRatio > 3) {
            videoSourceWidth = 16;
            videoSourceHeight = 9;
        }
        else if(aspectRatio < 0.3) {
            videoSourceHeight = 16;
            videoSourceWidth = 9;
        }

        val r = Rect();
        _player.getGlobalVisibleRect(r);
        r.right = r.right - _player.paddingEnd;
        val playpauseAction = if(_player.playing)
            RemoteAction(Icon.createWithResource(context, R.drawable.ic_pause_notif), context.getString(R.string.pause), context.getString(R.string.pauses_the_video), MediaControlReceiver.getPauseIntent(context, 5));
        else
            RemoteAction(Icon.createWithResource(context, R.drawable.ic_play_notif), context.getString(R.string.play), context.getString(R.string.resumes_the_video), MediaControlReceiver.getPlayIntent(context, 6));

        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(videoSourceWidth, videoSourceHeight))
            .setSourceRectHint(r)
            .setActions(listOf(playpauseAction))
            .build();
    }

    //Other
    private fun shareVideo() {
        Logger.i(TAG, "shareVideo")

        val url = video?.shareUrl ?: _searchVideo?.shareUrl ?: _url;
        fragment.startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND;
            putExtra(Intent.EXTRA_TEXT, url);
            type = "text/plain"; //TODO: Determine alt types?
        }, null));
    }

    private fun setLastPositionMilliseconds(positionMilliseconds: Long, updateHistory: Boolean) {
        lastPositionMilliseconds = positionMilliseconds;

        val v = video ?: return;
        val currentTime = System.currentTimeMillis();
        if (updateHistory && (_lastPositionSaveTime == -1L || currentTime - _lastPositionSaveTime > 5000)) {
            if (v !is TutorialFragment.TutorialVideo) {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val history = getHistoryIndex(v) ?: return@launch;
                    StateHistory.instance.updateHistoryPosition(v, history, true, (positionMilliseconds.toFloat() / 1000.0f).toLong());
                }
            }
            _lastPositionSaveTime = currentTime;
        }

        updateTracker(positionMilliseconds, isPlaying, false);
    }

    private fun updateTracker(positionMs: Long, isPlaying: Boolean, forceUpdate: Boolean = false) {
        val playbackTracker = _playbackTracker ?: return;
        val shouldUpdate = playbackTracker.shouldUpdate() || forceUpdate;
        if (!shouldUpdate) {
            return;
        }

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                playbackTracker.onProgress(positionMs.toDouble() / 1000, isPlaying);
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to notify progress.", e);
            }
        }
    }

    //Animation related setters
    fun setMinimizeProgress(progress : Float) {
        _minimizeProgress = progress;
        _player.lockControlsAlpha(progress < 0.9);
        _layoutPlayerContainer.shouldInterceptTouches = progress < 0.95;

        if(progress > 0.9) {
            if(_minimize_controls.visibility != View.GONE)
                _minimize_controls.visibility = View.GONE;
        }
        else if(_minimize_controls.visibility != View.VISIBLE) {
            _minimize_controls.visibility = View.VISIBLE;
        }

        //Switching video to fill
        if(progress > 0.25) {
            if(!_player.isFullScreen && _player.layoutParams.height != WRAP_CONTENT) {
                _player.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                if(!fragment.isInPictureInPicture) {
                    _player.fitHeight();
                    _layoutPlayerContainer.setPadding(0, 0, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6.0f, Resources.getSystem().displayMetrics).toInt());
                }
                else {
                    _layoutPlayerContainer.setPadding(0, 0, 0, 0);
                }
                _cast.layoutParams = _cast.layoutParams.apply {
                    (this as MarginLayoutParams).bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6.0f, resources.displayMetrics).toInt();
                };
                setProgressBarOverlayed(false);
                _player.hideControls(false);
            }
        }
        else {
            if(_player.layoutParams.height == WRAP_CONTENT) {
                _player.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                _player.fillHeight();
                _cast.layoutParams = _cast.layoutParams.apply {
                    (this as MarginLayoutParams).bottomMargin = 0;
                };
                setProgressBarOverlayed(true);
                _player.hideControls(false);

                _layoutPlayerContainer.setPadding(0, 0, 0, 0);
            }
        }
    }

    private fun setPolycentricProfile(cachedPolycentricProfile: PolycentricCache.CachedPolycentricProfile?, animate: Boolean) {
        _polycentricProfile = cachedPolycentricProfile;

        val dp_35 = 35.dp(context.resources)
        val profile = cachedPolycentricProfile?.profile;
        val avatar = profile?.systemState?.avatar?.selectBestImage(dp_35 * dp_35)
            ?.let { it.toURLInfoSystemLinkUrl(profile.system.toProto(), it.process, profile.systemState.servers.toList()) };

        if (avatar != null) {
            _creatorThumbnail.setThumbnail(avatar, animate);
        } else {
            _creatorThumbnail.setThumbnail(video?.author?.thumbnail, animate);
            _creatorThumbnail.setHarborAvailable(profile != null, animate, profile?.system?.toProto());
        }

        val username = cachedPolycentricProfile?.profile?.systemState?.username
        if (username != null) {
            _channelName.text = username
        }

        _monetization.setPolycentricProfile(cachedPolycentricProfile);
    }

    fun setProgressBarOverlayed(isOverlayed: Boolean?) {
        Logger.v(TAG, "setProgressBarOverlayed(isOverlayed: ${isOverlayed ?: "null"})");
        isOverlayed?.let{ _cast.setProgressBarOverlayed(it) };

        if(isOverlayed == null) {
            //For now this seems to be the best way to keep it updated?
            _playerProgress.layoutParams = _playerProgress.layoutParams.apply {
                (this as MarginLayoutParams).bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -12f, resources.displayMetrics).toInt();
            };
            _playerProgress.elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics);
        }
        else if(isOverlayed) {
            _playerProgress.layoutParams = _playerProgress.layoutParams.apply {
                (this as MarginLayoutParams).bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -2f, resources.displayMetrics).toInt();
            };
            _playerProgress.elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics);
        }
        else {
            _playerProgress.layoutParams = _playerProgress.layoutParams.apply {
                (this as MarginLayoutParams).bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt();
            };
            _playerProgress.elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics);
        }
    }
    fun setContentAlpha(alpha: Float) {
        _container_content.alpha = alpha;
    }
    fun setControllerAlpha(alpha: Float) {
        _layoutResume.alpha = alpha;
        _player.videoControls.alpha = alpha;
        _cast.setButtonAlpha(alpha);
    }
    fun setMinimizeControlsAlpha(alpha : Float) {
        _minimize_controls.alpha = alpha;
        val clickable = alpha > 0.9;
        if(_minimize_controls.isClickable != clickable)
            _minimize_controls.isClickable = clickable;
    }
    fun setVideoMinimize(value : Float) {
        val padRight = (resources.displayMetrics.widthPixels * 0.70 * value).toInt();
        _player.setPadding(0, _player.paddingTop, padRight, 0);
        _cast.setPadding(0, _cast.paddingTop, padRight, 0);
    }
    fun setTopPadding(value : Float) {
        _player.setPadding(0, value.toInt(), _player.paddingRight, 0);
    }

    //Tasks
    private val _taskLoadVideo = if(!isInEditMode) TaskHandler<String, IPlatformVideoDetails>(
        StateApp.instance.scopeGetter,
        {
            val result = StatePlatform.instance.getContentDetails(it).await();
            if(result !is IPlatformVideoDetails)
                throw IllegalStateException("Expected media content, found ${result.contentType}");
            return@TaskHandler result;
        })
        .success { setVideoDetails(it, true) }
        .exception<NoPlatformClientException> {
            Logger.w(TAG, "exception<NoPlatformClientException>", it)

            if (!nextVideo()) {
                UIDialogs.showDialog(context,
                    R.drawable.ic_sources,
                    "No source enabled to support this video\n(${_url})", null, null,
                    0,
                    UIDialogs.Action("Back", {
                        this@VideoDetailView.onClose.emit();
                    }, UIDialogs.ActionStyle.PRIMARY)
                );
            } else {
                StateAnnouncement.instance.registerAnnouncement(video?.id?.value + "_Q_NOSOURCES", context.getString(R.string.video_without_source), context.getString(R.string.there_was_a_in_your_queue_videoname_by_authorname_without_the_required_source_being_enabled_playback_was_skipped).replace("{videoName}", video?.name ?: "").replace("{authorName}", video?.author?.name ?: ""), AnnouncementType.SESSION)
            }
        }
        .exception<ScriptLoginRequiredException> { e ->
            Logger.w(TAG, "exception<ScriptLoginRequiredException>", e);

            UIDialogs.showDialog(context, R.drawable.ic_security, "Authentication", e.message, null, 0,
                UIDialogs.Action("Cancel", {}),
                UIDialogs.Action("Login", {
                    val id = e.config.let { if(it is SourcePluginConfig) it.id else null };
                    val didLogin = if(id == null)
                        false
                    else StatePlugins.instance.loginPlugin(context, id) {
                        fetchVideo();
                    }
                    if(!didLogin)
                        UIDialogs.showDialogOk(context, R.drawable.ic_error_pred, "Failed to login");
                }));
        }
        .exception<ContentNotAvailableYetException> {
            Logger.w(TAG, "exception<ContentNotAvailableYetException>", it)

            if (!nextVideo()) {
                UIDialogs.showSingleButtonDialog(context,
                    R.drawable.ic_schedule,
                    "Video is available in ${it.availableWhen}.",
                    "Back", {
                        this@VideoDetailView.onClose.emit();
                    });
            }
        }
        .exception<ScriptImplementationException> {
            Logger.w(TAG, "exception<ScriptImplementationException>", it)

            if (!nextVideo()) {
                UIDialogs.showGeneralRetryErrorDialog(context, context.getString(R.string.failed_to_load_video_scriptimplementationexception), it, ::fetchVideo);
            } else {
                StateAnnouncement.instance.registerAnnouncement(video?.id?.value + "_Q_INVALIDVIDEO", context.getString(R.string.invalid_video), context.getString(
                                    R.string.there_was_an_invalid_video_in_your_queue_videoname_by_authorname_playback_was_skipped).replace("{videoName}", video?.name ?: "").replace("{authorName}", video?.author?.name ?: ""), AnnouncementType.SESSION)
            }
        }
        .exception<ScriptAgeException> {
            Logger.w(TAG, "exception<ScriptAgeException>", it)

            if (!nextVideo()) {
                UIDialogs.showDialog(context,
                    R.drawable.ic_lock,
                    "Age restricted video",
                    it.message, null, 0,
                    UIDialogs.Action("Back", {
                        this@VideoDetailView.onClose.emit();
                    }, UIDialogs.ActionStyle.PRIMARY));
            } else {
                StateAnnouncement.instance.registerAnnouncement(video?.id?.value + "_Q_AGERESTRICT", context.getString(R.string.age_restricted_video),
                    context.getString(R.string.there_was_an_age_restricted_video_in_your_queue_videoname_by_authorname_this_video_was_not_accessible_and_playback_was_skipped).replace("{videoName}", video?.name ?: "").replace("{authorName}", video?.author?.name ?: ""),
                    AnnouncementType.SESSION)
            }
        }
        .exception<ScriptUnavailableException> {
            Logger.w(TAG, "exception<ScriptUnavailableException>", it);
            handleUnavailableVideo(it.message);
        }
        .exception<ScriptException> {
            Logger.w(TAG, "exception<ScriptException>", it)

            handleErrorOrCall {
                _retryCount = 0;
                _retryJob?.cancel();
                _retryJob = null;
                _liveTryJob?.cancel();
                _liveTryJob = null;
                UIDialogs.showGeneralRetryErrorDialog(context, context.getString(R.string.failed_to_load_video_scriptexception), it, ::fetchVideo);
            }
        }
        .exception<Throwable> {
            Logger.w(ChannelFragment.TAG, "Failed to load video.", it);

            handleErrorOrCall {
                _retryCount = 0;
                _retryJob?.cancel();
                _retryJob = null;
                _liveTryJob?.cancel();
                _liveTryJob = null;
                UIDialogs.showGeneralRetryErrorDialog(context, context.getString(R.string.failed_to_load_video), it, ::fetchVideo);
            }
        } else TaskHandler(IPlatformVideoDetails::class.java, {fragment.lifecycleScope});

    private val _taskLoadPolycentricProfile = TaskHandler<PlatformID, PolycentricCache.CachedPolycentricProfile?>(StateApp.instance.scopeGetter, { PolycentricCache.instance.getProfileAsync(it) })
        .success { it -> setPolycentricProfile(it, animate = true) }
        .exception<Throwable> {
            Logger.w(TAG, "Failed to load claims.", it);
        };

    private fun handleErrorOrCall(action: () -> Unit) {
        val isConnected = StateApp.instance.getCurrentNetworkState() != StateApp.NetworkState.DISCONNECTED;

        if (_retryCount < _retryIntervals.size) {
            Log.i(TAG, "handleErrorOrCall _retryCount=$_retryCount, starting retry job");

            _retryJob?.cancel();
            _retryJob = StateApp.instance.scopeOrNull?.launch(Dispatchers.Main) {
                try {
                    delay(_retryIntervals[_retryCount++] * 1000);
                    fetchVideo();
                } catch (e: Throwable) {
                    Logger.e(TAG, "Failed to retry fetch video.", e)
                }
            }
            _liveTryJob?.cancel();
            _liveTryJob = null;
        } else if (isConnected && nextVideo()) {
            Log.i(TAG, "handleErrorOrCall retries failed, is connected, skipped to next video");
        } else {
            Log.i(TAG, "handleErrorOrCall retries failed, no video to skip to, called action");
            action();
        }
    }

    private fun startLiveTry(liveTryVideo: IPlatformVideoDetails) {
        val datetime = liveTryVideo.datetime ?: return;
        val diffSeconds = datetime.getNowDiffSeconds();
        val toWait = _liveStreamCheckInterval.toList().sortedBy { abs(diffSeconds - it.first) }.firstOrNull()?.second?.toLong() ?: return;

        fragment.lifecycleScope.launch(Dispatchers.Main){
            UIDialogs.toast(context, context.getString(R.string.not_yet_available_retrying_in_time_s).replace("{time}", toWait.toString()));
        }

        _liveTryJob?.cancel();
        _liveTryJob = fragment.lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(toWait * 1000);
                val videoDetail = StatePlatform.instance.getContentDetails(liveTryVideo.url, true).await();
                if(videoDetail !is IPlatformVideoDetails)
                    throw IllegalStateException("Expected media content, found ${video?.contentType}");

                if(videoDetail.datetime != null && videoDetail.live == null && !videoDetail.video.videoSources.any()) {
                    if(videoDetail.datetime!! > OffsetDateTime.now())
                        withContext(Dispatchers.Main) {
                            UIDialogs.toast(context, context.getString(R.string.planned_in) + " ${videoDetail.datetime?.toHumanNowDiffString(true)}");
                        }
                    startLiveTry(liveTryVideo);
                }
                else
                    withContext(Dispatchers.Main) {
                        setVideoDetails(videoDetail, false);
                        _liveTryJob = null;
                    }
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Failed to live try fetch video.", ex);
                withContext(Dispatchers.Main) {
                    UIDialogs.toast(context, context.getString(R.string.failed_to_retry_for_live_stream));
                }
            }
        }
    }

    fun applyFragment(frag: VideoDetailFragment) {
        fragment = frag;
        fragment.onMinimize.subscribe {
            _liveChat?.stop();
            _container_content_liveChat.close();
        }
    }


    companion object {
        const val TAG_ADD = "add";
        const val TAG_BACKGROUND = "background";
        const val TAG_DOWNLOAD = "download";
        const val TAG_SHARE = "share";
        const val TAG_OVERLAY = "overlay";
        const val TAG_LIVECHAT = "livechat";
        const val TAG_OPEN = "open";
        const val TAG_MORE = "MORE";

        private val _buttonPinStore = FragmentedStorage.get<StringArrayStorage>("videoPinnedButtons");



    }
}
package com.futo.platformplayer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.lifecycle.lifecycleScope
import com.futo.platformplayer.activities.MainActivity
import com.futo.platformplayer.activities.ManageTabsActivity
import com.futo.platformplayer.activities.PolycentricHomeActivity
import com.futo.platformplayer.activities.PolycentricProfileActivity
import com.futo.platformplayer.activities.SettingsActivity
import com.futo.platformplayer.api.http.ManagedHttpClient
import com.futo.platformplayer.constructs.Event0
import com.futo.platformplayer.fragment.mainactivity.bottombar.MenuBottomBarFragment
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.serializers.FlexibleBooleanSerializer
import com.futo.platformplayer.serializers.OffsetDateTimeSerializer
import com.futo.platformplayer.states.StateAnnouncement
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StateBackup
import com.futo.platformplayer.states.StateCache
import com.futo.platformplayer.states.StateMeta
import com.futo.platformplayer.states.StatePayment
import com.futo.platformplayer.states.StatePolycentric
import com.futo.platformplayer.states.StateUpdate
import com.futo.platformplayer.stores.FragmentedStorage
import com.futo.platformplayer.stores.FragmentedStorageFileJson
import com.futo.platformplayer.views.FeedStyle
import com.futo.platformplayer.views.fields.DropdownFieldOptionsId
import com.futo.platformplayer.views.fields.FieldForm
import com.futo.platformplayer.views.fields.FormField
import com.futo.platformplayer.views.fields.FormFieldButton
import com.futo.platformplayer.views.fields.FormFieldWarning
import com.futo.platformplayer.views.overlays.slideup.SlideUpMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.OffsetDateTime

@Serializable
data class MenuBottomBarSetting(val id: Int, var enabled: Boolean);

@Serializable()
class Settings : FragmentedStorageFileJson() {
    var didFirstStart: Boolean = false;

    @Serializable
    val tabs: MutableList<MenuBottomBarSetting> = MenuBottomBarFragment.buttonDefinitions.map { MenuBottomBarSetting(it.id, true) }.toMutableList()

    @Transient
    val onTabsChanged = Event0();

    @FormField(R.string.manage_polycentric_identity, FieldForm.BUTTON, R.string.manage_your_polycentric_identity, -6)
    @FormFieldButton(R.drawable.ic_person)
    fun managePolycentricIdentity() {
        SettingsActivity.getActivity()?.let {
            if (StatePolycentric.instance.enabled) {
                if (StatePolycentric.instance.processHandle != null) {
                    it.startActivity(Intent(it, PolycentricProfileActivity::class.java));
                } else {
                    it.startActivity(Intent(it, PolycentricHomeActivity::class.java));
                }
            } else {
                UIDialogs.toast(it, "Polycentric is disabled")
            }
        }
    }

    @FormField(R.string.show_faq, FieldForm.BUTTON, R.string.get_answers_to_common_questions, -5)
    @FormFieldButton(R.drawable.ic_quiz)
    fun openFAQ() {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Settings.URL_FAQ))
            SettingsActivity.getActivity()?.startActivity(browserIntent);
        } catch (e: Throwable) {
            //Ignored
        }
    }
    @FormField(R.string.show_issues, FieldForm.BUTTON, R.string.a_list_of_user_reported_and_self_reported_issues, -4)
    @FormFieldButton(R.drawable.ic_data_alert)
    fun openIssues() {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/futo-org/grayjay-android/issues"))
            SettingsActivity.getActivity()?.startActivity(browserIntent);
        } catch (e: Throwable) {
            //Ignored
        }
    }

    /*
    @FormField(
        R.string.submit_feedback, FieldForm.BUTTON,
        R.string.give_feedback_on_the_application, -1
    )
    @FormFieldButton(R.drawable.ic_bug)
    fun submitFeedback() {
        try {
            val i = Intent(Intent.ACTION_VIEW);
            val subject = "Feedback Grayjay";
            val body = "Hey,\n\nI have some feedback on the Grayjay app.\nVersion information (version_name = ${BuildConfig.VERSION_NAME}, version_code = ${BuildConfig.VERSION_CODE}, flavor = ${BuildConfig.FLAVOR}, build_type = ${BuildConfig.BUILD_TYPE}})\n" +
                    "Device information (brand= ${Build.BRAND}, manufacturer = ${Build.MANUFACTURER}, device = ${Build.DEVICE}, version-sdk = ${Build.VERSION.SDK_INT}, version-os = ${Build.VERSION.BASE_OS})\n\n";
            val data = Uri.parse("mailto:grayjay@futo.org?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body));
            i.data = data;

            StateApp.withContext { it.startActivity(i); };
        } catch (e: Throwable) {
            //Ignored
        }
    }*/

    @FormField(R.string.manage_tabs, FieldForm.BUTTON, R.string.change_tabs_visible_on_the_home_screen, -3)
    @FormFieldButton(R.drawable.ic_tabs)
    fun manageTabs() {
        try {
            SettingsActivity.getActivity()?.let {
                it.startActivity(Intent(it, ManageTabsActivity::class.java));
            }
        } catch (e: Throwable) {
            //Ignored
        }
    }



    @FormField(R.string.import_data, FieldForm.BUTTON, R.string.import_data_description, -2)
    @FormFieldButton(R.drawable.ic_move_up)
    fun import() {
        val act = SettingsActivity.getActivity() ?: return;
        val intent = MainActivity.getImportOptionsIntent(act);
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK;
        act.startActivity(intent);
    }

    @FormField(R.string.link_handling, FieldForm.BUTTON, R.string.allow_grayjay_to_handle_links, -1)
    @FormFieldButton(R.drawable.ic_link)
    fun manageLinks() {
        try {
            SettingsActivity.getActivity()?.let { UIDialogs.showUrlHandlingPrompt(it) }
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to show url handling prompt", e)
        }
    }

    @FormField(R.string.language, "group", -1, 0)
    var language = LanguageSettings();
    @Serializable
    class LanguageSettings {
        @FormField(R.string.app_language, FieldForm.DROPDOWN, R.string.may_require_restart, 5, "app_language")
        @DropdownFieldOptionsId(R.array.app_languages)
        var appLanguage: Int = 0;

        fun getAppLanguageLocaleString(): String? {
            return when(appLanguage) {
                0 -> null
                1 -> "en";
                2 -> "de";
                3 -> "es";
                4 -> "pt";
                5 -> "fr"
                6 -> "ja";
                7 -> "ko";
                8 -> "zh";
                9 -> "ru";
                10 -> "ar";
                else -> null
            }
        }
    }

    @FormField(R.string.home, "group", R.string.configure_how_your_home_tab_works_and_feels, 1)
    var home = HomeSettings();
    @Serializable
    class HomeSettings {
        @FormField(R.string.feed_style, FieldForm.DROPDOWN, R.string.may_require_restart, 5)
        @DropdownFieldOptionsId(R.array.feed_style)
        var homeFeedStyle: Int = 1;

        fun getHomeFeedStyle(): FeedStyle {
            if(homeFeedStyle == 0)
                return FeedStyle.PREVIEW;
            else
                return FeedStyle.THUMBNAIL;
        }

        @FormField(R.string.preview_feed_items, FieldForm.TOGGLE, R.string.preview_feed_items_description, 6)
        var previewFeedItems: Boolean = true;


        @FormField(R.string.progress_bar, FieldForm.TOGGLE, R.string.progress_bar_description, 6)
        var progressBar: Boolean = true;


        @FormField(R.string.clear_hidden, FieldForm.BUTTON, R.string.clear_hidden_description, 8)
        @FormFieldButton(R.drawable.ic_visibility_off)
        fun clearHidden() {
            StateMeta.instance.removeAllHiddenCreators();
            StateMeta.instance.removeAllHiddenVideos();
            SettingsActivity.getActivity()?.let {
                UIDialogs.toast(it, "Creators and videos should show up again");
            }
        }
    }

    @FormField(R.string.search, "group", -1, 2)
    var search = SearchSettings();
    @Serializable
    class SearchSettings {
        @FormField(R.string.search_history, FieldForm.TOGGLE, R.string.may_require_restart, 3)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var searchHistory: Boolean = true;


        @FormField(R.string.feed_style, FieldForm.DROPDOWN, -1, 4)
        @DropdownFieldOptionsId(R.array.feed_style)
        var searchFeedStyle: Int = 1;

        @FormField(R.string.preview_feed_items, FieldForm.TOGGLE, R.string.preview_feed_items_description, 5)
        var previewFeedItems: Boolean = true;

        @FormField(R.string.progress_bar, FieldForm.TOGGLE, R.string.progress_bar_description, 6)
        var progressBar: Boolean = true;


        fun getSearchFeedStyle(): FeedStyle {
            if(searchFeedStyle == 0)
                return FeedStyle.PREVIEW;
            else
                return FeedStyle.THUMBNAIL;
        }
    }


    @FormField(R.string.channel, "group", -1, 3)
    var channel = ChannelSettings();
    @Serializable
    class ChannelSettings {

        @FormField(R.string.progress_bar, FieldForm.TOGGLE, R.string.progress_bar_description, 6)
        var progressBar: Boolean = true;
    }

    @FormField(R.string.subscriptions, "group", R.string.configure_how_your_subscriptions_works_and_feels, 4)
    var subscriptions = SubscriptionsSettings();
    @Serializable
    class SubscriptionsSettings {
        @FormField(R.string.feed_style, FieldForm.DROPDOWN, R.string.may_require_restart, 4)
        @DropdownFieldOptionsId(R.array.feed_style)
        var subscriptionsFeedStyle: Int = 1;

        fun getSubscriptionsFeedStyle(): FeedStyle {
            if(subscriptionsFeedStyle == 0)
                return FeedStyle.PREVIEW;
            else
                return FeedStyle.THUMBNAIL;
        }

        @FormField(R.string.show_subscription_group, FieldForm.TOGGLE, R.string.show_subscription_group_description, 5)
        var showSubscriptionGroups: Boolean = true;

        @FormField(R.string.preview_feed_items, FieldForm.TOGGLE, R.string.preview_feed_items_description, 6)
        var previewFeedItems: Boolean = true;

        @FormField(R.string.progress_bar, FieldForm.TOGGLE, R.string.progress_bar_description, 7)
        var progressBar: Boolean = true;

        @FormField(R.string.fetch_on_app_boot, FieldForm.TOGGLE, R.string.shortly_after_opening_the_app_start_fetching_subscriptions, 8)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var fetchOnAppBoot: Boolean = true;

        @FormField(R.string.fetch_on_tab_opened, FieldForm.TOGGLE, R.string.fetch_on_tab_opened_description, 9)
        var fetchOnTabOpen: Boolean = true;

        @FormField(R.string.background_update, FieldForm.DROPDOWN, R.string.experimental_background_update_for_subscriptions_cache, 10, "background_update")
        @DropdownFieldOptionsId(R.array.background_interval)
        var subscriptionsBackgroundUpdateInterval: Int = 0;

        fun getSubscriptionsBackgroundIntervalMinutes(): Int = when(subscriptionsBackgroundUpdateInterval) {
            0 -> 0;
            1 -> 15;
            2 -> 60;
            3 -> 60 * 3;
            4 -> 60 * 6;
            5 -> 60 * 12;
            6 -> 60 * 24;
            else -> 0
        };


        @FormField(R.string.subscription_concurrency, FieldForm.DROPDOWN, R.string.specify_how_many_threads_are_used_to_fetch_channels, 11)
        @DropdownFieldOptionsId(R.array.thread_count)
        var subscriptionConcurrency: Int = 3;

        fun getSubscriptionsConcurrency() : Int {
            return threadIndexToCount(subscriptionConcurrency);
        }

        @FormField(R.string.show_watch_metrics, FieldForm.TOGGLE, R.string.show_watch_metrics_description, 12)
        var showWatchMetrics: Boolean = false;

        @FormField(R.string.track_playtime_locally, FieldForm.TOGGLE, R.string.track_playtime_locally_description, 13)
        var allowPlaytimeTracking: Boolean = true;


        @FormField(R.string.always_reload_from_cache, FieldForm.TOGGLE, R.string.always_reload_from_cache_description, 14)
        var alwaysReloadFromCache: Boolean = false;

        @FormField(R.string.clear_channel_cache, FieldForm.BUTTON, R.string.clear_channel_cache_description, 15)
        fun clearChannelCache() {
            UIDialogs.toast(SettingsActivity.getActivity()!!, "Started clearing..");
            StateCache.instance.clear();
            UIDialogs.toast(SettingsActivity.getActivity()!!, "Finished clearing");
        }
    }

    @FormField(R.string.player, "group", R.string.change_behavior_of_the_player, 5)
    var playback = PlaybackSettings();
    @Serializable
    class PlaybackSettings {
        @FormField(R.string.primary_language, FieldForm.DROPDOWN, -1, 0)
        @DropdownFieldOptionsId(R.array.audio_languages)
        var primaryLanguage: Int = 0;

        fun getPrimaryLanguage(context: Context): String? {
            return when(primaryLanguage) {
                0 -> "en";
                1 -> "es";
                2 -> "de";
                3 -> "fr";
                4 -> "ja";
                5 -> "ko";
                6 -> "th";
                7 -> "vi";
                8 -> "id";
                9 -> "hi";
                10 -> "ar";
                11 -> "tu";
                12 -> "ru";
                13 -> "pt";
                14 -> "zh";
                else -> null
            }
        }

        //= context.resources.getStringArray(R.array.audio_languages)[primaryLanguage];

        @FormField(R.string.default_playback_speed, FieldForm.DROPDOWN, -1, 1)
        @DropdownFieldOptionsId(R.array.playback_speeds)
        var defaultPlaybackSpeed: Int = 3;
        fun getDefaultPlaybackSpeed(): Float = when(defaultPlaybackSpeed) {
            0 -> 0.25f;
            1 -> 0.5f;
            2 -> 0.75f;
            3 -> 1.0f;
            4 -> 1.25f;
            5 -> 1.5f;
            6 -> 1.75f;
            7 -> 2.0f;
            8 -> 2.25f;
            else -> 1.0f;
        };

        @FormField(R.string.preferred_quality, FieldForm.DROPDOWN, R.string.preferred_quality_description, 2)
        @DropdownFieldOptionsId(R.array.preferred_quality_array)
        var preferredQuality: Int = 0;

        @FormField(R.string.preferred_metered_quality, FieldForm.DROPDOWN, R.string.preferred_metered_quality_description, 3)
        @DropdownFieldOptionsId(R.array.preferred_quality_array)
        var preferredMeteredQuality: Int = 0;
        fun getPreferredQualityPixelCount(): Int = preferedQualityToPixels(preferredQuality);
        fun getPreferredMeteredQualityPixelCount(): Int = preferedQualityToPixels(preferredMeteredQuality);
        fun getCurrentPreferredQualityPixelCount(): Int = if(!StateApp.instance.isCurrentMetered()) getPreferredQualityPixelCount() else getPreferredMeteredQualityPixelCount();

        @FormField(R.string.preferred_preview_quality, FieldForm.DROPDOWN, R.string.preferred_preview_quality_description, 4)
        @DropdownFieldOptionsId(R.array.preferred_quality_array)
        var preferredPreviewQuality: Int = 5;
        fun getPreferredPreviewQualityPixelCount(): Int = preferedQualityToPixels(preferredPreviewQuality);

        @FormField(R.string.auto_rotate, FieldForm.DROPDOWN, -1, 5)
        @DropdownFieldOptionsId(R.array.system_enabled_disabled_array)
        var autoRotate: Int = 2;

        fun isAutoRotate() = autoRotate == 1 || (autoRotate == 2 && StateApp.instance.getCurrentSystemAutoRotate());

        @FormField(R.string.auto_rotate_dead_zone, FieldForm.DROPDOWN, R.string.this_prevents_the_device_from_rotating_within_the_given_amount_of_degrees, 6)
        @DropdownFieldOptionsId(R.array.auto_rotate_dead_zone)
        var autoRotateDeadZone: Int = 0;

        fun getAutoRotateDeadZoneDegrees(): Int {
            return autoRotateDeadZone * 5;
        }

        @FormField(R.string.background_behavior, FieldForm.DROPDOWN, -1, 7)
        @DropdownFieldOptionsId(R.array.player_background_behavior)
        var backgroundPlay: Int = 2;

        fun isBackgroundContinue() = backgroundPlay == 1;
        fun isBackgroundPictureInPicture() = backgroundPlay == 2;

        @FormField(R.string.resume_after_preview, FieldForm.DROPDOWN, R.string.when_watching_a_video_in_preview_mode_resume_at_the_position_when_opening_the_video_code, 7)
        @DropdownFieldOptionsId(R.array.resume_after_preview)
        var resumeAfterPreview: Int = 1;

        fun shouldResumePreview(previewedPosition: Long): Boolean{
            if(resumeAfterPreview == 2)
                return true;
            if(resumeAfterPreview == 1 && previewedPosition > 10)
                return true;
            return false;
        }

        @FormField(R.string.chapter_update_fps_title, FieldForm.DROPDOWN, R.string.chapter_update_fps_description, 8)
        @DropdownFieldOptionsId(R.array.chapter_fps)
        var chapterUpdateFPS: Int = 0;

        fun getChapterUpdateFrames(): Int {
            return when(chapterUpdateFPS) {
                0 -> 24
                1 -> 30
                2 -> 60
                3 -> 120
                else -> 1
            };
        }

        @FormField(R.string.live_chat_webview, FieldForm.TOGGLE, R.string.use_the_live_chat_web_window_when_available_over_native_implementation, 9)
        var useLiveChatWindow: Boolean = true;



        @FormField(R.string.background_switch_audio, FieldForm.TOGGLE, R.string.background_switch_audio_description, 10)
        var backgroundSwitchToAudio: Boolean = true;

        @FormField(R.string.restart_after_audio_focus_loss, FieldForm.DROPDOWN, R.string.restart_playback_when_gaining_audio_focus_after_a_loss, 11)
        @DropdownFieldOptionsId(R.array.restart_playback_after_loss)
        var restartPlaybackAfterLoss: Int = 1;

        @FormField(R.string.restart_after_connectivity_loss, FieldForm.DROPDOWN, R.string.restart_playback_when_gaining_connectivity_after_a_loss, 12)
        @DropdownFieldOptionsId(R.array.restart_playback_after_loss)
        var restartPlaybackAfterConnectivityLoss: Int = 1;

        @FormField(R.string.full_screen_portrait, FieldForm.TOGGLE, R.string.allow_full_screen_portrait, 13)
        var fullscreenPortrait: Boolean = false;
    }

    @FormField(R.string.comments, "group", R.string.comments_description, 6)
    var comments = CommentSettings();
    @Serializable
    class CommentSettings {
        @FormField(R.string.default_comment_section, FieldForm.DROPDOWN, -1, 0)
        @DropdownFieldOptionsId(R.array.comment_sections)
        var defaultCommentSection: Int = 0;

        @FormField(R.string.bad_reputation_comments_fading, FieldForm.TOGGLE, R.string.bad_reputation_comments_fading_description, 0)
        var badReputationCommentsFading: Boolean = true;
    }

    @FormField(R.string.downloads, "group", R.string.configure_downloading_of_videos, 7)
    var downloads = Downloads();
    @Serializable
    class Downloads {

        @FormField(R.string.download_when, FieldForm.DROPDOWN, R.string.configure_when_videos_should_be_downloaded, 0)
        @DropdownFieldOptionsId(R.array.when_download)
        var whenDownload: Int = 0;

        fun shouldDownload(): Boolean {
            return when (whenDownload) {
                0 -> !StateApp.instance.isCurrentMetered();
                1 -> StateApp.instance.isNetworkState(StateApp.NetworkState.WIFI, StateApp.NetworkState.ETHERNET);
                2 -> true;
                else -> false;
            }
        }

        @FormField(R.string.default_video_quality, FieldForm.DROPDOWN, -1, 2)
        @DropdownFieldOptionsId(R.array.preferred_video_download)
        var preferredVideoQuality: Int = 4;
        fun getDefaultVideoQualityPixels(): Int = preferedQualityToPixels(preferredVideoQuality);

        @FormField(R.string.default_audio_quality, FieldForm.DROPDOWN, -1, 3)
        @DropdownFieldOptionsId(R.array.preferred_audio_download)
        var preferredAudioQuality: Int = 1;
        fun isHighBitrateDefault(): Boolean = preferredAudioQuality > 0;

        @FormField(R.string.byte_range_download, FieldForm.TOGGLE, R.string.attempt_to_utilize_byte_ranges, 4)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var byteRangeDownload: Boolean = true;

        @FormField(R.string.byte_range_concurrency, FieldForm.DROPDOWN, R.string.number_of_concurrent_threads_to_multiply_download_speeds_from_throttled_sources, 5)
        @DropdownFieldOptionsId(R.array.thread_count)
        var byteRangeConcurrency: Int = 3;
        fun getByteRangeThreadCount(): Int {
            return threadIndexToCount(byteRangeConcurrency);
        }
    }

    @FormField(R.string.browsing, "group", R.string.configure_browsing_behavior, 8)
    var browsing = Browsing();
    @Serializable
    class Browsing {
        @FormField(R.string.enable_video_cache, FieldForm.TOGGLE, R.string.cache_to_quickly_load_previously_fetched_videos, 0)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var videoCache: Boolean = true;

        @FormField(R.string.show_alternative_metadata_by_default_label, FieldForm.TOGGLE, R.string.show_alternative_metadata_by_default_description, 0)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var showAlternativeMetadataByDefault: Boolean = true;
    }

    @FormField(R.string.casting, "group", R.string.configure_casting, 9)
    var casting = Casting();
    @Serializable
    class Casting {
        @FormField(R.string.enabled, FieldForm.TOGGLE, R.string.enable_casting, 0)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var enabled: Boolean = true;

        @FormField(R.string.keep_screen_on, FieldForm.TOGGLE, R.string.keep_screen_on_while_casting, 1)
        @Serializable(with = FlexibleBooleanSerializer::class)
        var keepScreenOn: Boolean = true;

        /*TODO: Should we have a different casting quality?
        @FormField("Preferred Casting Quality", FieldForm.DROPDOWN, "", 3)
        @DropdownFieldOptionsId(R.array.preferred_quality_array)
        var preferredQuality: Int = 4;
        fun getPreferredQualityPixelCount(): Int {
            when (preferredQuality) {
                0 -> return 1280 * 720;
                1 -> return 3840 * 2160;
                2 -> return 1920 * 1080;
                3 -> return 1280 * 720;
                4 -> return 640 * 480;
                else -> return 0;
            }
        }*/
    }

    @FormField(R.string.logging, FieldForm.GROUP, -1, 10)
    var logging = Logging();
    @Serializable
    class Logging {
        @FormField(R.string.log_level, FieldForm.DROPDOWN, -1, 0)
        @DropdownFieldOptionsId(R.array.log_levels)
        var logLevel: Int = 0;

        @FormField(R.string.submit_logs, FieldForm.BUTTON, R.string.submit_logs_to_help_us_narrow_down_issues, 1)
        fun submitLogs() {
            StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                try {
                    if (!Logger.submitLogs()) {
                        withContext(Dispatchers.Main) {
                            SettingsActivity.getActivity()?.let { UIDialogs.toast(it, it.getString(R.string.please_enable_logging_to_submit_logs)) }
                        }
                    }
                } catch (e: Throwable) {
                    Logger.e("Settings", "Failed to submit logs.", e);
                }
            }
        }
    }

    @FormField(R.string.announcement, FieldForm.GROUP, -1, 11)
    var announcementSettings = AnnouncementSettings();
    @Serializable
    class AnnouncementSettings {
        @FormField(R.string.reset_announcements, FieldForm.BUTTON, R.string.reset_hidden_announcements, 1)
        fun resetAnnouncements() {
            StateAnnouncement.instance.resetAnnouncements();
            SettingsActivity.getActivity()?.let { UIDialogs.toast(it, it.getString(R.string.announcements_reset)); };
        }
    }

    @FormField(R.string.notifications, FieldForm.GROUP, -1, 12)
    var notifications = NotificationSettings();
    @Serializable
    class NotificationSettings {
        @FormField(R.string.planned_content_notifications, FieldForm.TOGGLE, R.string.planned_content_notifications_description, 1)
        var plannedContentNotification: Boolean = true;
    }

    @FormField(R.string.plugins, FieldForm.GROUP, -1, 13)
    @Transient
    var plugins = Plugins();
    @Serializable
    class Plugins {

        @FormField(R.string.clear_cookies_on_logout, FieldForm.TOGGLE, R.string.clears_cookies_when_you_log_out, 0)
        var clearCookiesOnLogout: Boolean = true;

        @FormField(R.string.clear_cookies, FieldForm.BUTTON, R.string.clears_in_app_browser_cookies, 1)
        fun clearCookies() {
            val cookieManager: CookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
        }
        /*@FormField(R.string.reinstall_embedded_plugins, FieldForm.BUTTON, R.string.also_removes_any_data_related_plugin_like_login_or_settings, 1)
        fun reinstallEmbedded() {
            StateApp.instance.scopeOrNull!!.launch(Dispatchers.IO) {
                try {
                    StatePlugins.instance.reinstallEmbeddedPlugins(StateApp.instance.context);

                    withContext(Dispatchers.Main) {
                        StateApp.instance.contextOrNull?.let {
                            UIDialogs.toast(it, it.getString(R.string.embedded_plugins_reinstalled_a_reboot_is_recommended));
                        };
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        StateApp.withContext {
                            UIDialogs.toast(it, "Failed: " + ex.message);
                        };
                    }
                }
            }
        }*/
    }


    @FormField(R.string.external_storage, FieldForm.GROUP, -1, 14)
    var storage = Storage();
    @Serializable
    class Storage {
        var storage_general: String? = null;
        var storage_download: String? = null;

        fun getStorageGeneralUri(): Uri? = storage_general?.let { Uri.parse(it) };
        fun getStorageDownloadUri(): Uri? = storage_download?.let { Uri.parse(it) };
        fun isStorageMainValid(context: Context): Boolean = StateApp.instance.isValidStorageUri(context, getStorageGeneralUri());
        fun isStorageDownloadValid(context: Context): Boolean = StateApp.instance.isValidStorageUri(context, getStorageDownloadUri());

        @FormField(R.string.change_external_general_directory, FieldForm.BUTTON, R.string.change_the_external_directory_for_general_files, 3)
        fun changeStorageGeneral() {
            SettingsActivity.getActivity()?.let {
                StateApp.instance.changeExternalGeneralDirectory(it);
            }
        }
        @FormField(R.string.change_external_downloads_directory, FieldForm.BUTTON, R.string.change_the_external_storage_for_download_files, 4)
        fun changeStorageDownload() {
            SettingsActivity.getActivity()?.let {
                StateApp.instance.changeExternalDownloadDirectory(it);
            }
        }

        @FormField(R.string.clear_external_downloads_directory, FieldForm.BUTTON, R.string.clear_the_external_storage_for_download_files, 5)
        fun clearStorageDownload() {
            Settings.instance.storage.storage_download = null;
            Settings.instance.save();
            SettingsActivity.getActivity()?.let { UIDialogs.toast(it, "Cleared download storage directory") };
        }
    }


    @FormField(R.string.auto_update, "group", R.string.configure_the_auto_updater, 15)
    var autoUpdate = AutoUpdate();
    @Serializable
    class AutoUpdate {
        @FormField(R.string.check, FieldForm.DROPDOWN, -1, 0)
        @DropdownFieldOptionsId(R.array.auto_update_when_array)
        var check: Int = 0;

        @FormField(R.string.background_download, FieldForm.DROPDOWN, R.string.configure_if_background_download_should_be_used, 1)
        @DropdownFieldOptionsId(R.array.background_download)
        var backgroundDownload: Int = 0;

        @FormField(R.string.download_when, FieldForm.DROPDOWN, R.string.configure_when_updates_should_be_downloaded, 2)
        @DropdownFieldOptionsId(R.array.when_download)
        var whenDownload: Int = 0;

        fun shouldDownload(): Boolean {
            return when (whenDownload) {
                0 -> !StateApp.instance.isCurrentMetered();
                1 -> StateApp.instance.isNetworkState(StateApp.NetworkState.WIFI, StateApp.NetworkState.ETHERNET);
                2 -> true;
                else -> false;
            }
        }

        fun isAutoUpdateEnabled(): Boolean {
            return check == 0 && !BuildConfig.IS_PLAYSTORE_BUILD;
        }

        @FormField(R.string.manual_check, FieldForm.BUTTON, R.string.manually_check_for_updates, 3)
        fun manualCheck() {
            if (!BuildConfig.IS_PLAYSTORE_BUILD) {
                SettingsActivity.getActivity()?.let {
                    StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                        StateUpdate.instance.checkForUpdates(it, true)
                    }
                }
            } else {
                SettingsActivity.getActivity()?.let {
                    try {
                        it.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${it.packageName}")))
                    } catch (e: ActivityNotFoundException) {
                        UIDialogs.toast(it, it.getString(R.string.failed_to_show_store));
                    }
                }
            }
        }

        @FormField(R.string.view_changelog, FieldForm.BUTTON, R.string.review_the_current_and_past_changelogs, 4)
        fun viewChangelog() {
            SettingsActivity.getActivity()?.let {
                UIDialogs.toast(it.getString(R.string.retrieving_changelog));

                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    try {
                        val version = StateUpdate.instance.downloadVersionCode(ManagedHttpClient()) ?: return@launch;
                        Logger.i(TAG, "Version retrieved $version");

                        withContext(Dispatchers.Main) {
                            UIDialogs.showChangelogDialog(it, version);
                        }
                    } catch (e: Throwable) {
                        Logger.e("Settings", "Failed to submit logs.", e);
                    }
                }
            };
        }

        @FormField(R.string.remove_cached_version, FieldForm.BUTTON, R.string.remove_the_last_downloaded_version, 5)
        fun removeCachedVersion() {
            StateApp.withContext {
                val outputDirectory = File(it.filesDir, "autoupdate");
                if (!outputDirectory.exists()) {
                    UIDialogs.toast("Directory does not exist");
                    return@withContext;
                }

                File(outputDirectory, "last_version.apk").delete();
                File(outputDirectory, "last_version.txt").delete();
                UIDialogs.toast("Removed downloaded version");
            }
        }
    }

    @FormField(R.string.backup, FieldForm.GROUP, -1, 16)
    var backup = Backup();
    @Serializable
    class Backup {
        @Serializable(with = OffsetDateTimeSerializer::class)
        var lastAutoBackupTime: OffsetDateTime = OffsetDateTime.MIN;
        var didAskAutoBackup: Boolean = false;
        var autoBackupPassword: String? = null;
        fun shouldAutomaticBackup() = autoBackupPassword != null;

        @FormField(R.string.automatic_backup, FieldForm.READONLYTEXT, -1, 0)
        val automaticBackupText get() = if(!shouldAutomaticBackup()) "None" else "Every Day";

        @FormField(R.string.set_automatic_backup, FieldForm.BUTTON, R.string.configure_daily_backup_in_case_of_catastrophic_failure, 1)
        fun configureAutomaticBackup() {
            UIDialogs.showAutomaticBackupDialog(SettingsActivity.getActivity()!!, autoBackupPassword != null) {
                SettingsActivity.getActivity()?.reloadSettings();
            };
        }
        @FormField(R.string.restore_automatic_backup, FieldForm.BUTTON, R.string.restore_a_previous_automatic_backup, 2)
        fun restoreAutomaticBackup() {
            val activity = SettingsActivity.getActivity()!!

            if(!StateBackup.hasAutomaticBackup())
                UIDialogs.toast(activity, activity.getString(R.string.you_don_t_have_any_automatic_backups), false);
            else
                UIDialogs.showAutomaticRestoreDialog(activity, activity.lifecycleScope);
        }


        @FormField(R.string.export_data, FieldForm.BUTTON, R.string.creates_a_zip_file_with_your_data_which_can_be_imported_by_opening_it_with_grayjay, 3)
        fun export() {
            val activity = SettingsActivity.getActivity() ?: return;
            UISlideOverlays.showOverlay(activity.overlay, "Select export type", null, {},
                SlideUpMenuItem(activity, R.drawable.ic_share, "Share", "", null, {
                    StateBackup.shareExternalBackup();
                }),
                SlideUpMenuItem(activity, R.drawable.ic_download, "File", "", null, {
                    StateBackup.saveExternalBackup(activity);
                })
            )
        }
    }

    @FormField(R.string.payment, FieldForm.GROUP, -1, 17)
    var payment = Payment();
    @Serializable
    class Payment {
        @FormField(R.string.payment_status, FieldForm.READONLYTEXT, -1, 1)
        val paymentStatus: String get() = SettingsActivity.getActivity()?.let { if (StatePayment.instance.hasPaid) it.getString(R.string.paid) else it.getString(R.string.not_paid); } ?: "Unknown";

        @FormField(R.string.clear_payment, FieldForm.BUTTON, R.string.deletes_license_keys_from_app, 2)
        fun clearPayment() {
            StatePayment.instance.clearLicenses();
            SettingsActivity.getActivity()?.let {
                UIDialogs.toast(it, it.getString(R.string.licenses_cleared_might_require_app_restart));
                it.reloadSettings();
            }
        }
    }

    @FormField(R.string.other, FieldForm.GROUP, -1, 18)
    var other = Other();
    @Serializable
    class Other {
        @FormField(R.string.bypass_rotation_prevention, FieldForm.TOGGLE, R.string.bypass_rotation_prevention_description, 1)
        @FormFieldWarning(R.string.bypass_rotation_prevention_warning)
        var bypassRotationPrevention: Boolean = false;

        @FormField(R.string.enable_polycentric, FieldForm.TOGGLE, R.string.can_be_disabled_when_you_are_experiencing_issues, 1)
        var polycentricEnabled: Boolean = true;
    }

    @FormField(R.string.gesture_controls, FieldForm.GROUP, -1, 19)
    var gestureControls = GestureControls();
    @Serializable
    class GestureControls {
        @FormField(R.string.volume_slider, FieldForm.TOGGLE, R.string.volume_slider_descr, 1)
        var volumeSlider: Boolean = true;

        @FormField(R.string.brightness_slider, FieldForm.TOGGLE, R.string.brightness_slider_descr, 2)
        var brightnessSlider: Boolean = true;

        @FormField(R.string.toggle_full_screen, FieldForm.TOGGLE, R.string.toggle_full_screen_descr, 3)
        var toggleFullscreen: Boolean = true;

        @FormField(R.string.system_brightness, FieldForm.TOGGLE, R.string.system_brightness_descr, 4)
        var useSystemBrightness: Boolean = false;

        @FormField(R.string.system_volume, FieldForm.TOGGLE, R.string.system_volume_descr, 5)
        var useSystemVolume: Boolean = true;

        @FormField(R.string.restore_system_brightness, FieldForm.TOGGLE, R.string.restore_system_brightness_descr, 6)
        var restoreSystemBrightness: Boolean = true;

        @FormField(R.string.zoom_option, FieldForm.TOGGLE, R.string.zoom_option_descr, 7)
        var zoom: Boolean = true;

        @FormField(R.string.pan_option, FieldForm.TOGGLE, R.string.pan_option_descr, 8)
        var pan: Boolean = true;
    }

    @FormField(R.string.info, FieldForm.GROUP, -1, 20)
    var info = Info();
    @Serializable
    class Info {
        @FormField(R.string.version_code, FieldForm.READONLYTEXT, -1, 1, "code")
        var versionCode = BuildConfig.VERSION_CODE;
        @FormField(R.string.version_name, FieldForm.READONLYTEXT, -1, 2)
        var versionName = BuildConfig.VERSION_NAME;
        @FormField(R.string.version_type, FieldForm.READONLYTEXT, -1, 3)
        var versionType = BuildConfig.BUILD_TYPE;
    }

    //region BOILERPLATE
    override fun encode(): String {
        return Json.encodeToString(this);
    }

    companion object {
        private const val TAG = "Settings";
        const val URL_FAQ = "https://grayjay.app/faq.html";

        private var _isFirst = true;

        val instance: Settings get() {
            if(_isFirst) {
                Logger.i(TAG, "Initial Settings fetch");
                _isFirst = false;
            }
            return FragmentedStorage.get<Settings>();
        }

        fun replace(text: String) {
            FragmentedStorage.replace<Settings>(text, true);
        }


        private fun preferedQualityToPixels(q: Int): Int {
            when (q) {
                0 -> return 1280 * 720;
                1 -> return 3840 * 2160;
                2 -> return 2560 * 1440;
                3 -> return 1920 * 1080;
                4 -> return 1280 * 720;
                5 -> return 854 * 480;
                6 -> return 640 * 360;
                7 -> return 426 * 240;
                8 -> return 256 * 144;
                else -> return 0;
            }
        }


        private fun threadIndexToCount(index: Int): Int {
            return when(index) {
                0 -> 1;
                1 -> 2;
                2 -> 4;
                3 -> 6;
                4 -> 8;
                5 -> 10;
                6 -> 15;
                else -> 1
            }
        }
    }
    //endregion
}
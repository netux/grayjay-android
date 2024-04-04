package com.futo.platformplayer.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.futo.platformplayer.*
import com.futo.platformplayer.api.http.ManagedHttpClient
import com.futo.platformplayer.constructs.TaskHandler
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.others.Version
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StateUpdate

class ChangelogDialog(context: Context?) : AlertDialog(context) {
    companion object {
        private val TAG = "ChangelogDialog";

        // TODO(netux): get max patch per version, somehow
        private const val MAX_VERSION_FORK_PATCH = 10;
    }

    private lateinit var _textVersion: TextView;
    private lateinit var _textChangelog: TextView;
    private lateinit var _buttonPrevious: Button;
    private lateinit var _buttonNext: Button;
    private lateinit var _buttonClose: Button;
    private lateinit var _buttonUpdate: LinearLayout;
    private lateinit var _imageSpinner: ImageView;
    private var _isLoading: Boolean = false;
    private var _version: Version = Version(0, 0);
    private var _maxVersion: Version = Version(0, 0);
    private var _managedHttpClient = ManagedHttpClient();

    private val _taskDownloadChangelog = TaskHandler<Version, String?>(StateApp.instance.scopeGetter, { version -> StateUpdate.instance.downloadChangelog(_managedHttpClient, version) })
        .success { setChangelog(it); }
        .exception<Throwable> {
            Logger.w(TAG, "Failed to load changelog.", it);
            setChangelog(null);
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_changelog, null));

        _textVersion = findViewById(R.id.text_version);
        _textChangelog = findViewById(R.id.text_changelog);
        _buttonPrevious = findViewById(R.id.button_previous);
        _buttonNext = findViewById(R.id.button_next);
        _buttonClose = findViewById(R.id.button_close);
        _buttonUpdate = findViewById(R.id.button_update);
        _imageSpinner = findViewById(R.id.image_spinner);

        _textChangelog.movementMethod = ScrollingMovementMethod();

        _buttonPrevious.setOnClickListener {
            var prevVersion =
                if (_version.forkMinor == 0) Version(_version.upstreamMajor - 1, MAX_VERSION_FORK_PATCH)
                else Version(_version.upstreamMajor, _version.forkMinor - 1);
            if (prevVersion <= Version(0, 0)) {
                prevVersion = Version(0, 0);
            }
            setVersion(prevVersion);
        };

        _buttonNext.setOnClickListener {
            var nextVersion =
                if (_version.forkMinor >= MAX_VERSION_FORK_PATCH) Version(_version.upstreamMajor + 1, 0)
                else Version(_version.upstreamMajor, _version.forkMinor + 1);
            if (nextVersion >= _maxVersion) {
                nextVersion = _maxVersion;
            }
            setVersion(nextVersion);
        };

        _buttonClose.setOnClickListener {
            dismiss();
        };

        _buttonUpdate.setOnClickListener {
            UIDialogs.showUpdateAvailableDialog(context, _maxVersion);
            dismiss();
        };
    }

    override fun dismiss() {
        _taskDownloadChangelog.cancel();
        super.dismiss()
    }

    fun setMaxVersion(version: Version) {
        _maxVersion = version;
        setVersion(version);

        val currentVersion = BuildConfig.FULL_VERSION_CODE;
        _buttonUpdate.visibility = if (currentVersion == _maxVersion) View.GONE else View.VISIBLE;
    }

    private fun setVersion(version: Version) {
        if (_version == version) {
            return;
        }

        _version = version;
        _buttonPrevious.visibility = if (_version == Version(0, 0)) View.GONE else View.VISIBLE;
        _buttonNext.visibility = if (_version == _maxVersion) View.GONE else View.VISIBLE;
        _textVersion.text = _version.toString();
        setIsLoading(true);
        _taskDownloadChangelog.run(_version);
    }

    private fun setChangelog(text: String?) {
        _textChangelog.text = text ?: "There is no changelog available for this version.";
        setIsLoading(false);
    }

    private fun setIsLoading(isLoading: Boolean) {
        if (isLoading) {
            _imageSpinner.visibility = View.VISIBLE;
            _textChangelog.visibility = View.GONE;
            (_imageSpinner.drawable as Animatable?)?.start();
        } else {
            (_imageSpinner.drawable as Animatable?)?.stop();
            _imageSpinner.visibility = View.GONE;
            _textChangelog.visibility = View.VISIBLE;
        }

        _isLoading = false;
    }
}
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
    }

    private lateinit var _textVersion: TextView;
    private lateinit var _textChangelog: TextView;
    private lateinit var _buttonPreviousMajor: Button;
    private lateinit var _buttonPreviousMinor: Button;
    private lateinit var _buttonNextMajor: Button;
    private lateinit var _buttonNextMinor: Button;
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
        _buttonPreviousMajor = findViewById(R.id.button_previous_major);
        _buttonPreviousMinor = findViewById(R.id.button_previous_minor);
        _buttonNextMajor = findViewById(R.id.button_next_major);
        _buttonNextMinor = findViewById(R.id.button_next_minor);
        _buttonClose = findViewById(R.id.button_close);
        _buttonUpdate = findViewById(R.id.button_update);
        _imageSpinner = findViewById(R.id.image_spinner);

        _textChangelog.movementMethod = ScrollingMovementMethod();

        _buttonPreviousMajor.setOnClickListener {
            setVersion(Version(Math.max(0, _version.upstreamMajor - 1), 0));
        };

        _buttonPreviousMinor.setOnClickListener {
            var major = _version.upstreamMajor;
            var minor = _version.forkMinor - 1;
            if (minor < 0) {
                major -= 1;
                minor = 0;
            }
            setVersion(Version(major, minor));
        };

        _buttonNextMajor.setOnClickListener {
            var nextVersion = Version(_version.upstreamMajor + 1, 0);
            if (nextVersion > _maxVersion) {
                nextVersion = _maxVersion;
            }
            setVersion(nextVersion);
        };

        _buttonNextMinor.setOnClickListener {
            var nextVersion = Version(_version.upstreamMajor, _version.forkMinor + 1);
            if (nextVersion > _maxVersion) {
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
        _buttonPreviousMajor.visibility = if (_version.upstreamMajor <= 0) View.GONE else View.VISIBLE;
        _buttonPreviousMinor.visibility = if (_version <= Version(0, 0)) View.GONE else View.VISIBLE;
        _buttonNextMajor.visibility = if (_version.upstreamMajor >= _maxVersion.upstreamMajor) View.GONE else View.VISIBLE;
        _buttonNextMinor.visibility = if (_version >= _maxVersion) View.GONE else View.VISIBLE;
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
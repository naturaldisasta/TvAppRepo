package news.androidtv.tvapprepo.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import news.androidtv.tvapprepo.R;
import news.androidtv.tvapprepo.download.ApkDownloadHelper;
import news.androidtv.tvapprepo.model.AdvancedOptions;

/**
 * Created by Nick Felker on 3/20/2017.
 */
public class GenerateShortcutHelper {
    private static final String KEY_BUILD_STATUS = "build_ok";
    private static final String KEY_APP_OBJ = "app";
    private static final String KEY_DOWNLOAD_URL = "download_link";

    public static void begin(final Activity activity, final ResolveInfo resolveInfo) {
        new AlertDialog.Builder(new android.view.ContextThemeWrapper(activity, R.style.dialog_theme))
                .setTitle("Create Launcher Shortcut for " +
                        resolveInfo.activityInfo.applicationInfo.loadLabel(activity.getPackageManager()) + "?")
                .setMessage("A shortcut will be generated and installed. If installed, " +
                        "it will place a banner on your homescreen. When clicked, it will " +
                        "launch the app. This shortcut does not replace the app, rather it " +
                        "just acts as a bridge between the Leanback Launcher and the non-Leanback app.")
                .setPositiveButton("Create Shortcut", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        generateShortcut(activity, resolveInfo);
                    }
                })
                .setNeutralButton("Advanced", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open a new dialog
                        openAdvancedOptions(activity, resolveInfo);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void openAdvancedOptions(final Activity activity, final ResolveInfo resolveInfo) {
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Advanced Options")
                .setView(R.layout.dialog_app_shortcut_editor)
                .setNegativeButton("Cancel", null)
                .show();
        dialog.setButton(Dialog.BUTTON_POSITIVE, "Create Shortcut", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                View editor = dialog.getWindow().getDecorView();
                AdvancedOptions options = new AdvancedOptions(activity);
                String bannerUrl =
                        ((EditText) editor.findViewById(R.id.edit_banner)).getText().toString();
                boolean isGame = ((Switch) editor.findViewById(R.id.switch_isgame)).isChecked();
                options.setBannerUrl(bannerUrl).setIsGame(isGame);
                generateShortcut(activity, resolveInfo, options);
            }
        });
    }

    private static void downloadShortcutApk(Activity activity, NetworkResponse response, Object item) {
        JSONObject data = null;
        try {
            data = new JSONObject(new String(response.data));
            if (data.getBoolean(KEY_BUILD_STATUS)) {
                String downloadLink = data.getJSONObject(KEY_APP_OBJ).getString(KEY_DOWNLOAD_URL);
                ApkDownloadHelper apkDownloadHelper = new ApkDownloadHelper(activity);

                if (activity == null) {
                    throw new NullPointerException("Activity variable doesn't exist");
                }
                apkDownloadHelper.startDownload(downloadLink,
                        ((ResolveInfo) item).activityInfo.applicationInfo
                                .loadLabel(activity.getPackageManager()).toString());
            } else {
                Toast.makeText(activity, "Build failed", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            throw new NullPointerException(e.getMessage() +
                    "\nSomething odd is happening for " +
                    ((ResolveInfo) item).activityInfo.packageName
                    + "\n" + data.toString());
        }
    }

    private static void generateShortcut(final Activity activity, final ResolveInfo resolveInfo) {
        generateShortcut(activity, resolveInfo, null);
    }

    private static void generateShortcut(final Activity activity, final ResolveInfo resolveInfo,
            final AdvancedOptions options) {
        if (!options.isReady()) {
            // Delay until we complete all web operations
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    generateShortcut(activity, resolveInfo, options);
                }
            }, 200);
            return;
        }
        Toast.makeText(activity,
                "Please wait. This may take up to 20 seconds.",
                Toast.LENGTH_SHORT).show();
        ShortcutPostTask.generateShortcut(activity,
                resolveInfo,
                options,
                new ShortcutPostTask.Callback() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        // TODO Hide ad
                        downloadShortcutApk(activity, response, resolveInfo);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Toast.makeText(activity,
                                "Build failed: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
        // TODO Show visual ad
    }
}
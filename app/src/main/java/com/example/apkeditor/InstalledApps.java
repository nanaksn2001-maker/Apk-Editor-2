package com.nama.paketmu; // sesuaikan

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledApps {

    public static class App {
        public String label;
        public String packageName;
        public Drawable icon;
        public boolean isSystem;
    }

    // Panggil dari thread background, jangan dari UI thread
    public static List<App> load(PackageManager pm) {
        List<App> result = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(0)) {
            App app = new App();
            app.label = info.loadLabel(pm).toString();
            app.packageName = info.packageName;
            app.icon = info.loadIcon(pm);
            app.isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            result.add(app);
        }
        Collections.sort(result, (a, b) ->
                a.label.compareToIgnoreCase(b.label));
        return result;
    }

    // Salin base.apk ke folder kerja, lalu kembalikan File-nya
    public static File importApk(PackageManager pm, String pkg, File workDir)
            throws IOException, PackageManager.NameNotFoundException {
        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
        File out = new File(workDir, pkg + ".apk");
        copy(new File(info.sourceDir), out);
        return out;
    }

    private static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream os = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        }
    }
}

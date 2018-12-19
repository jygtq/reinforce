package com.example.reinforce.shell;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.ArrayMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

/**
 * 作者：created by wujin on 2018/12/15
 * 邮箱：jin.wu@geely.com
 */
public class ProxyApplication extends Application {

    /**
     * 源APP Application名称
     */
    private static final String SOURCE_APPLICATION_NAME = "com.example.reinforce.source.MyApplication";
    /**
     * 源APP APK文件路径
     */
    private String sourceApkFilePath;
    /**
     * 用于存放源APP so文件
     */
    private String libPath;

    /**
     * 资源管理器
     */
    private AssetManager mAssetManager;
    /**
     * 资源
     */
    private Resources mResources;
    /**
     * 主题
     */
    private Resources.Theme mTheme;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // 创建两个文件夹source_apk、source_libs，存放源apk和lib
            File sourceApkFile = this.getDir("source_apk", Context.MODE_PRIVATE);
            File sourceLibsFile = this.getDir("source_libs", Context.MODE_PRIVATE);
            String sourceApkPath = sourceApkFile.getAbsolutePath();
            libPath = sourceLibsFile.getAbsolutePath();
            sourceApkFilePath = sourceApkFile.getAbsolutePath() + "/source.apk";
            File dexFile = new File(sourceApkFilePath);


            if (!dexFile.exists()) {
                // 在source_apk文件夹内，创建source.apk
                dexFile.createNewFile();
                // 读取本程序classes.dex文件
                byte[] dexData = this.readDexFileFromApk();
                // 将合并的dex文件分离，取出源apk文件已用于动态加载
                this.splitPayLoadFromDex(dexData);
            }


            //----------将当前进程的 DexClassLoader 替换成源apk的 DexClassLoader

            // 反射获取当前 ActivityThread 对象
            Object currentActivityThread = RefInvoke.invokeStaticMethod(
                    "android.app.ActivityThread", "currentActivityThread",
                    new Class[] {}, new Object[] {});
            // 反射获取 ActivityThread 中 mPackages
            ArrayMap mPackages = (ArrayMap) RefInvoke.getFieldObject(
                    "android.app.ActivityThread", currentActivityThread,
                    "mPackages");

            // 当前进程 DexClassLoader 弱引用
            WeakReference wr = (WeakReference) mPackages.get(getPackageName());
            // 创建源apk的DexClassLoader对象，加载源apk内的类和本地代码（c/c++代码）
            DexClassLoader dLoader = new DexClassLoader(this.sourceApkFilePath, sourceApkPath,
                    libPath, (ClassLoader) RefInvoke.getFieldObject(
                    "android.app.LoadedApk", wr.get(), "mClassLoader"));
            // 把当前进程的 DexClassLoader 设置成源apk的 DexClassLoader
            RefInvoke.setFieldObject("android.app.LoadedApk", "mClassLoader",
                    wr.get(), dLoader);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {

        // 加载源apk资源文件
        loadResources(sourceApkFilePath);

        // 反射获取当前 ActivityThread 对象
        Object currentActivityThread = RefInvoke.invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[] {}, new Object[] {});
        // 反射获取 currentActivityThread 的 mBoundApplication 属性
        Object mBoundApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        // 反射获取 mBoundApplication 的 info 属性（当前进程 mApplication 对象）
        Object loadedApkInfo = RefInvoke.getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        // 将当前进程的 mApplication 设置成null
        RefInvoke.setFieldObject("android.app.LoadedApk", "mApplication",
                loadedApkInfo, null);



        // 反射获取 ActivityThread 中 mInitialApplication 对象
        Object oldApplication = RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");
        // 反射获取 ActivityThread 中的 Application 列表
        List<Application> mAllApplications = (List<Application>) RefInvoke
                .getFieldObject("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        // 删除 mAllApplications 中的 oldApplication
        mAllApplications.remove(oldApplication);



        // 反射获取 LoadedApk 中 mApplicationInfo 对象
        ApplicationInfo appInfoInLoadedApk = (ApplicationInfo) RefInvoke
                .getFieldObject("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        // 反射获取 AppBindData 中 appInfo 对象
        ApplicationInfo appInfoInAppBindData = (ApplicationInfo) RefInvoke
                .getFieldObject("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");
        // 替换为源apk Application
        appInfoInLoadedApk.className = SOURCE_APPLICATION_NAME;
        appInfoInAppBindData.className = SOURCE_APPLICATION_NAME;


        // 反射执行 makeApplication（false, null），获取源apk 中 application 对象
        Application app = (Application) RefInvoke.invokeMethod(
                "android.app.LoadedApk", "makeApplication", loadedApkInfo,
                new Class[] { boolean.class, Instrumentation.class },
                new Object[] { false, null });

        // 反射替换 application 为源apk 中 application 对象
        RefInvoke.setFieldObject("android.app.ActivityThread",
                "mInitialApplication", currentActivityThread, app);
        ArrayMap mProviderMap = (ArrayMap) RefInvoke.getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        // 遍历替换 application 为源apk 中 application 对象
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = RefInvoke.getFieldObject(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldObject("android.content.ContentProvider",
                    "mContext", localProvider, app);
        }

        // 调用源apk application onCreate() 方法
        app.onCreate();
    }

    /**
     * 将合并的dex文件分离，取出源apk文件已用于动态加载
     * @param dexData 当前程序dex文件
     * @throws IOException
     */
    private void splitPayLoadFromDex(byte[] dexData) throws IOException {
        int dexLen = dexData.length;
        // 取被加壳apk的长度
        byte[] apkLenByte = new byte[4];
        // 将末尾4个字节的源apk长度值复制到apkLenByte
        System.arraycopy(dexData, dexLen - 4, apkLenByte, 0, 4);

        // 读取源apk
        ByteArrayInputStream bais = new ByteArrayInputStream(apkLenByte);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        byte[] sourceApkByte = new byte[readInt];
        // 把被源apk拷贝到sourceApkByte中
        System.arraycopy(dexData, dexLen - 4 - readInt, sourceApkByte, 0, readInt);
        // 对源程序Apk进行解密
        sourceApkByte = decrypt(sourceApkByte);
        // 写入源apk文件
        File file = new File(sourceApkFilePath);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(sourceApkByte);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        // 解压源apk文件，将源apk中so文件放入source_libs中
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));

        while (true) {
            // 遍历目录
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            // 取出源apk用到的so文件，放到 libPath 中（data/data/包名/source_libs)
            String name = localZipEntry.getName();
            if (name.startsWith("lib/") && name.endsWith(".so")) {
                File storeFile = new File(libPath + "/"
                        + name.substring(name.lastIndexOf('/')));
                storeFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(storeFile);
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1){
                        break;
                    }
                    fos.write(arrayOfByte, 0, i);
                }
                fos.flush();
                fos.close();
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
    }

    /**
     * 获取壳工程的dex文件内容（byte）
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)  {
                        break;
                    }
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }

    /**
     * 解密源apk
     * @param apkData
     * @return
     */
    private byte[] decrypt(byte[] apkData) {
        for (int i = 0;i < apkData.length; i++){
            apkData[i] = (byte) (0xFF ^ apkData[i]);
        }
        return apkData;
    }

    /**
     * 加载源apk资源文件
     *
     * @param sourceApkFilePath Apk文件目录
     */
    private void loadResources(String sourceApkFilePath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod(
                    "addAssetPath", String.class);
            addAssetPath.invoke(assetManager, sourceApkFilePath);
            mAssetManager = assetManager;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Resources superRes = super.getResources();
        superRes.getDisplayMetrics();
        superRes.getConfiguration();
        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),
                superRes.getConfiguration());
        mTheme = mResources.newTheme();
        mTheme.setTo(super.getTheme());
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }
    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }
    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }
}

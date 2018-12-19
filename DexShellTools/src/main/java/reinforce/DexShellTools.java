package reinforce;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

public class DexShellTools {

    public static void main(String[] args) {
        try {
            // 需要加固的apk文件
            File sourceApkFile = new File("DexShellFile/source.apk");
            // 以二进制形式读出源Apk，并进行加密处理
            byte[] sourceApkByte = encryptApk(readFileBytes(sourceApkFile));


            // 解壳dex
            File unShellDexFile = new File("DexShellFile/shell.dex");
            // 以二进制形式读出dex
            byte[] unShellDexByte = readFileBytes(unShellDexFile);


            // 总长度（apk文件 + dex文件 + 4，多出4字节是存放长度的。）
            int totalLen = sourceApkByte.length + unShellDexByte.length + 4;

            // 申请了总长度byte[]
            byte[] newDexByte = new byte[totalLen];

            // 先将解壳dex放入newDexByte中
            System.arraycopy(unShellDexByte, 0, newDexByte, 0, unShellDexByte.length);
            // 再在dex内容后面拷贝apk的内容
            System.arraycopy(sourceApkByte, 0, newDexByte, unShellDexByte.length, sourceApkByte.length);
            // 最后添加解壳数据长度
            System.arraycopy(intToByte(sourceApkByte.length), 0, newDexByte, totalLen - 4, 4);

            // 修改 newDex file size 文件头
            fixFileSizeHeader(newDexByte);
            // 修改 newDex SHA1 文件头
            fixSHA1Header(newDexByte);
            // 修改 newDex CheckSum 文件头
            fixCheckSumHeader(newDexByte);

            // 将新的dex文件写入本地
            String str = "DexShellFile/classes.dex";
            File file = new File(str);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream localFileOutputStream = new FileOutputStream(str);
            localFileOutputStream.write(newDexByte);
            localFileOutputStream.flush();
            localFileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * apk加密
     *
     * @param data
     * @return
     */
    private static byte[] encryptApk(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (0xFF ^ data[i]);
        }
        return data;
    }

    /**
     * 修改dex头，CheckSum 校验码
     *
     * @param dexBytes
     */
    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        // 从12到文件末尾计算校验码
        adler.update(dexBytes, 12, dexBytes.length - 12);
        long value = adler.getValue();
        int va = (int) value;
        byte[] newcs = intToByte(va);
        // 高位在前，低位在前掉个个
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length - 1 - i];
            System.out.println(Integer.toHexString(newcs[i]));
        }
        // 效验码赋值（8-11）
        System.arraycopy(recs, 0, dexBytes, 8, 4);
    }

    /**
     * int 转byte[]
     *
     * @param number
     * @return
     */
    public static byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    /**
     * 修改dex头 sha1值
     *
     * @param dexBytes
     * @throws NoSuchAlgorithmException
     */
    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        //从32为到结束计算sha--1
        md.update(dexBytes, 32, dexBytes.length - 32);
        byte[] newdt = md.digest();
        //修改sha-1值（12-31）
        System.arraycopy(newdt, 0, dexBytes, 12, 20);
        // 输出sha-1值，可有可无
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }
        System.out.println(hexstr);
    }

    /**
     * 修改dex头 file_size值
     *
     * @param dexBytes
     */
    private static void fixFileSizeHeader(byte[] dexBytes) {
        // 新文件长度
        byte[] newfs = intToByte(dexBytes.length);
        System.out.println(Integer.toHexString(dexBytes.length));
        byte[] refs = new byte[4];
        // 高位在前，低位在前掉个个
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length - 1 - i];
            System.out.println(Integer.toHexString(newfs[i]));
        }
        // 修改（32-35）
        System.arraycopy(refs, 0, dexBytes, 32, 4);
    }

    /**
     * 以二进制读出文件内容
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }
}

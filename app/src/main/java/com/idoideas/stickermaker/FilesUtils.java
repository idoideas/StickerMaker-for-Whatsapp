package com.idoideas.stickermaker;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FilesUtils {
    public static void handleCopyFilesToSdCard(final Context context, String id) {
        File targetFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name)
                );
        deleteAllFiles(targetFolder);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        try {
            exportFile(new File(context.getFilesDir()+"/"+id+"/"+id+".zip"),targetFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteAllFiles(File targetFolder) {
        File[] filesList = targetFolder.listFiles();
        if (filesList == null) {
            return;
        }
        for (File file : filesList) {
            if (file.isDirectory()) {
                deleteAllFiles(file);
                file.delete();
            } else {
                file.delete();
            }
        }

    }



    private static File exportFile(File src, File dst) throws IOException {

        //if folder does not exist
        if (!dst.exists()) {
            if (!dst.mkdir()) {
                return null;
            }
        }

        File expFile = new File(dst.getPath() + File.separator + src.getPath().split("/")[src.getPath().split("/").length-1]);
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(expFile).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }

        return expFile;
    }

    public static String inputStreamToSavedFile(InputStream input, Context context, String filename){
        File targetFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name)
        );
        deleteAllFiles(targetFolder);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name)
                , filename);
        try {
            file.createNewFile();
            OutputStream output = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[4 * 1024]; // or other buffer size
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                output.flush();
                return file.getAbsolutePath();
            } finally {
                output.close();
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }   finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return file.getAbsolutePath();
        }
    }

    public static String getActualIDOfPack(String path){
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if(files[i].getName().contains(".json")){
                return files[i].getName().replace(".json", "");
            }
        }
        return null;
    }


    public static int getUriSize(Uri uri, Context context) throws IOException {
        try (final InputStream inputStream = context.getContentResolver().openInputStream(uri);
             final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("cannot get URI SIZE");
            }
            int read;
            byte[] data = new byte[16384];

            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }
            return buffer.toByteArray().length;
        }
    }


}

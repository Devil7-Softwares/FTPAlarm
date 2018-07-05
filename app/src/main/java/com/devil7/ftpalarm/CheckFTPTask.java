package com.devil7.ftpalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

public class CheckFTPTask extends AsyncTask<Context, Void, Integer> {

    private static CheckFTPTask mInstance = null;
    private final Handler mHandler = new WaitDialogHandler();
    private Context mContext;
    private boolean mIsBackgroundThread;

    private ResultListener resultListener;

    public interface ResultListener {
        void Result(int count);
    }
    
    private CheckFTPTask(boolean isBackgroundThread) {
        this.mIsBackgroundThread = isBackgroundThread;
    }

    public static CheckFTPTask getInstance(boolean isBackgroundThread) {
        if (mInstance == null) {
            mInstance = new CheckFTPTask(isBackgroundThread);
        }
        mInstance.mIsBackgroundThread = isBackgroundThread;
        return mInstance;
    }

    private static boolean isConnectivityAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    protected Integer doInBackground(Context... params) {
        mContext = params[0];

        if (!isConnectivityAvailable(mContext)) {
            return 0;
        }

        showWaitDialog();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());

        String server = preferences.getString(MainActivity.APP_SERVER, "127.0.0.1");
        int port = Integer.parseInt(preferences.getString(MainActivity.APP_PORT, "21"));
        String user = preferences.getString(MainActivity.APP_USERNAME, "");
        String pass = preferences.getString(MainActivity.APP_PASSWORD, "");

        //int count = 0;
        IntegerHolder mCount = new IntegerHolder();
        mCount.value = 0;

        FTPClient ftpClient = new FTPClient();

        try {

            ftpClient.connect(server, port);
            showServerReply(ftpClient);

            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                Log.e("FTP","Connect failed");
                return 0;
            }

            boolean success = ftpClient.login(user, pass);
            showServerReply(ftpClient);

            if (!success) {
                Log.e("FTP","Could not login to the server");
                return 0;
            }

            // uses simpler methods
            String[] files2 = ftpClient.listNames();
            if(files2 != null){
                //count += files2.length;
                listDirectory(ftpClient,"/","",0,mCount,3);
            }else{
                ftpClient.enterLocalPassiveMode();
                files2 = ftpClient.listNames();
                showServerReply(ftpClient);
                if (files2 != null) listDirectory(ftpClient,"/","",0,mCount,3);
            }


        } catch (IOException ex) {
            Log.e("FTP","Oops! Something wrong happened");
            ex.printStackTrace();
        } finally {
            // logs out and disconnects from server
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                    showServerReply(ftpClient);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        //return count;
        Log.e("FTP",mCount.value.toString());
        return mCount.value;
    }

    class IntegerHolder {
        public Integer value;
    }

    static void listDirectory(FTPClient ftpClient, String parentDir,
                              String currentDir, int level, IntegerHolder mCount, Integer MaxDepth) throws IOException {
        if (level == MaxDepth) return;
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }
        FTPFile[] subFiles = ftpClient.listDirectories(dirToList);
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".")
                        || currentFileName.equals("..")) {
                    continue;
                }
                for (int i = 0; i < level; i++) {
                    System.out.print("\t");
                }
                if (aFile.isDirectory()) {
                    mCount.value +=1;
                    listDirectory(ftpClient, dirToList, currentFileName, level + 1, mCount, MaxDepth);
                }
            }
        }
    }

    @Override
    protected void onPostExecute(Integer count) {
        super.onPostExecute(count);
        hideWaitDialog();
        if(this.resultListener != null){
            this.resultListener.Result(count);
        }
        mInstance = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mInstance = null;
    }

    public void setOnResultListner(ResultListener listener){
        this.resultListener = listener;
    }

    private void showWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_SHOW_DIALOG);
            msg.obj = mContext;
            msg.arg1 = R.string.dialog_message;
            mHandler.sendMessage(msg);
        }
    }

    private void hideWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_CLOSE_DIALOG);
            mHandler.sendMessage(msg);
        }
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                Log.i("SERVER" , aReply);
            }
        }
    }
}

package com.jiushuo.uavRct;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jiushuo.uavRct.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

public class PlaybackActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "图库活动";

    private Button mBackBtn, mDeleteBtn, mReloadBtn, mDownloadBtn, mStatusBtn;
    private Button mPlayBtn, mResumeBtn, mPauseBtn, mStopBtn, mMoveToBtn;
    private RecyclerView listView;
    private FileListAdapter mListAdapter;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private FetchMediaTaskScheduler scheduler;
    private ProgressDialog mLoadingDialog;
    private ProgressDialog mDownloadDialog;
    private SlidingDrawer mPushDrawerSd;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/WaypointMission/album");
    private int currentProgress = -1;
    private ImageView mDisplayImageView;
    private int lastClickViewIndex = -1;
    private View lastClickView;
    private TextView mPushTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        initUI();
        Log.w(TAG, "onCreate: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMediaManager();
        Log.w(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        lastClickView = null;
        if (mMediaManager != null) {
            mMediaManager.stop(null);
            mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mMediaManager.removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener);
            mMediaManager.exitMediaDownloading();
            if (scheduler != null) {
                scheduler.removeAllTasks();
            }
        }

        DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError mError) {
                if (mError != null) {
                    setResultToToast("Set Shoot Photo Mode Failed" + mError.getDescription());
                }
            }
        });

        if (mediaFileList != null) {
            mediaFileList.clear();
        }
        super.onDestroy();
        Log.w(TAG, "onDestroy: ");
    }

    void initUI() {

        //Init RecyclerView
        listView = (RecyclerView) findViewById(R.id.filelistView);
        @SuppressLint("WrongConstant") LinearLayoutManager layoutManager = new LinearLayoutManager(PlaybackActivity.this, OrientationHelper.VERTICAL, false);
        listView.setLayoutManager(layoutManager);

        //Init FileListAdapter
        mListAdapter = new FileListAdapter();
        listView.setAdapter(mListAdapter);

        //Init Loading Dialog
        mLoadingDialog = new ProgressDialog(PlaybackActivity.this);
        mLoadingDialog.setMessage("Please wait");
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);

        //Init Download Dialog
        mDownloadDialog = new ProgressDialog(PlaybackActivity.this);
        mDownloadDialog.setTitle("Downloading file");
        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mMediaManager != null) {
                    mMediaManager.exitMediaDownloading();
                }
            }
        });

        mPushDrawerSd = (SlidingDrawer) findViewById(R.id.pointing_drawer_sd);
        mPushTv = (TextView) findViewById(R.id.pointing_push_tv);
        mBackBtn = (Button) findViewById(R.id.back_btn);
        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mReloadBtn = (Button) findViewById(R.id.reload_btn);
        mStatusBtn = (Button) findViewById(R.id.status_btn);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mStopBtn = (Button) findViewById(R.id.stop_btn);
        mMoveToBtn = (Button) findViewById(R.id.moveTo_btn);
        mDisplayImageView = (ImageView) findViewById(R.id.imageView);
        mDisplayImageView.setVisibility(View.VISIBLE);

        mBackBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mReloadBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mStatusBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mMoveToBtn.setOnClickListener(this);

    }

    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mLoadingDialog != null) {
                    mLoadingDialog.show();
                }
            }
        });
    }

    private void hideProgressDialog() {

        runOnUiThread(new Runnable() {
            public void run() {
                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });
    }

    private void ShowDownloadProgressDialog() {
        if (mDownloadDialog != null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
                    mDownloadDialog.show();
                }
            });
        }
    }

    private void HideDownloadProgressDialog() {

        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.dismiss();
                }
            });
        }
    }

    private void setResultToToast(final String result) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(PlaybackActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setResultToText(final String string) {
        if (mPushTv == null) {
            setResultToToast("Push info tv has not be init...");
        }
        PlaybackActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushTv.setText(string);
            }
        });
    }

    private void initMediaManager() {
        if (DemoApplication.getProductInstance() == null) {
            mediaFileList.clear();
            mListAdapter.notifyDataSetChanged();
            DJILog.e(TAG, "Product disconnected");
            return;
        } else {
            if (null != DemoApplication.getCameraInstance() && DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
                if (null != mMediaManager) {
                    mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
                    mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
                    DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                DJILog.e(TAG, "Set cameraMode success");
                                showProgressDialog();
                                getFileList();
                            } else {
                                setResultToToast("Set cameraMode failed");
                            }
                        }
                    });
                    if (mMediaManager.isVideoPlaybackSupported()) {
                        DJILog.e(TAG, "Camera support video playback!");
                    } else {
                        setResultToToast("Camera does not support video playback!");
                    }
                    scheduler = mMediaManager.getScheduler();
                }

            } else if (null != DemoApplication.getCameraInstance()
                    && !DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                setResultToToast("Media Download Mode not Supported");
            }
        }
        return;
    }

    private void getFileList() {
        mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
        if (mMediaManager != null) {

            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                DJILog.e(TAG, "Media Manager is busy.");
            } else {

                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            hideProgressDialog();

                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                                lastClickViewIndex = -1;
                                lastClickView = null;
                            }

                            mediaFileList = mMediaManager.getSDCardFileListSnapshot();
                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
                                @Override
                                public int compare(MediaFile lhs, MediaFile rhs) {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1;
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
                            scheduler.resume(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    if (error == null) {
                                        getThumbnails();
                                    }
                                }
                            });
                        } else {
                            hideProgressDialog();
                            setResultToToast("Get Media File List Failed:" + djiError.getDescription());
                        }
                    }
                });

            }
        }
    }

    private void getThumbnails() {
        if (mediaFileList.size() <= 0) {
            setResultToToast("No File info for downloading thumbnails");
            return;
        }
        for (int i = 0; i < mediaFileList.size(); i++) {
            getThumbnailByIndex(i);
        }
    }

    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
            if (null == error) {
                if (option == FetchMediaTaskContent.PREVIEW) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
                if (option == FetchMediaTaskContent.THUMBNAIL) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            } else {
                DJILog.e(TAG, "Fetch Media Task Failed" + error.getDescription());
            }
        }
    };

    private void getThumbnailByIndex(final int index) {
        FetchMediaTask task = new FetchMediaTask(mediaFileList.get(index), FetchMediaTaskContent.THUMBNAIL, taskCallback);
        scheduler.moveTaskToEnd(task);
    }

    private class ItemHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail_img;
        TextView file_name;
        TextView fileCreateTime;
        TextView file_size;
        TextView file_time;

        public ItemHolder(View itemView) {
            super(itemView);
            this.thumbnail_img = (ImageView) itemView.findViewById(R.id.filethumbnail);
            this.file_name = (TextView) itemView.findViewById(R.id.filename);
            this.fileCreateTime = (TextView) itemView.findViewById(R.id.fileCreateTime);
            this.file_size = (TextView) itemView.findViewById(R.id.fileSize);
            this.file_time = (TextView) itemView.findViewById(R.id.filetime);
        }
    }

    private class FileListAdapter extends RecyclerView.Adapter<ItemHolder> {
        @Override
        public int getItemCount() {
            if (mediaFileList != null) {
                return mediaFileList.size();
            }
            return 0;
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_info_item, parent, false);
            return new ItemHolder(view);
        }

        /**
         * 用于对RecyclerView子项的数据进行赋值，
         * 会在每个子项被滚动到屏幕内的时候执行
         *
         * @param mItemHolder
         * @param index
         */
        @Override
        public void onBindViewHolder(ItemHolder mItemHolder, final int index) {

            final MediaFile mediaFile = mediaFileList.get(index);
            if (mediaFile != null) {
                if (mediaFile.getMediaType() != MediaFile.MediaType.MOV && mediaFile.getMediaType() != MediaFile.MediaType.MP4) {
                    mItemHolder.file_time.setVisibility(View.GONE);
                } else {
                    mItemHolder.file_time.setVisibility(View.VISIBLE);
                    mItemHolder.file_time.setText(mediaFile.getDurationInSeconds() + " s");
                }
                mItemHolder.file_name.setText(mediaFile.getFileName());
                mItemHolder.fileCreateTime.setText(mediaFile.getDateCreated());
                mItemHolder.file_size.setText(getFormattedFileSize(mediaFile.getFileSize()));
                mItemHolder.thumbnail_img.setImageBitmap(mediaFile.getThumbnail());
                mItemHolder.thumbnail_img.setOnClickListener(ImgOnClickListener);
                mItemHolder.thumbnail_img.setTag(mediaFile);
                mItemHolder.itemView.setTag(index);

                if (lastClickViewIndex == index) {
                    mItemHolder.itemView.setSelected(true);
                } else {
                    mItemHolder.itemView.setSelected(false);
                }
                mItemHolder.itemView.setOnClickListener(itemViewOnClickListener);

            }
        }
    }

    private String getFormattedFileSize(long fileSize) {
        if (fileSize < 1024) {
            return fileSize + "B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f", fileSize / 1024.0) + "KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f", fileSize / (1024.0 * 1024)) + "MB";
        } else {
            return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024)) + "GB";
        }
    }

    private View.OnClickListener itemViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            lastClickViewIndex = (int) (v.getTag());

            if (lastClickView != null && lastClickView != v) {
                lastClickView.setSelected(false);
            }
            v.setSelected(true);
            lastClickView = v;
        }
    };

    private View.OnClickListener ImgOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaFile selectedMedia = (MediaFile) v.getTag();
            if (selectedMedia != null && mMediaManager != null) {
                addMediaTask(selectedMedia);
            }
        }
    };

    private void addMediaTask(final MediaFile mediaFile) {
        final FetchMediaTaskScheduler scheduler = mMediaManager.getScheduler();
        final FetchMediaTask task =
                new FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW, new FetchMediaTask.Callback() {
                    @Override
                    public void onUpdate(final MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError error) {
                        if (null == error) {
                            if (mediaFile.getPreview() != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Bitmap previewBitmap = mediaFile.getPreview();
                                        mDisplayImageView.setVisibility(View.VISIBLE);
                                        mDisplayImageView.setImageBitmap(previewBitmap);
                                    }
                                });
                            } else {
                                setResultToToast("null bitmap!");
                            }
                        } else {
                            setResultToToast("fetch preview image failed: " + error.getDescription());
                        }
                    }
                });

        scheduler.resume(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    scheduler.moveTaskToNext(task);
                } else {
                    setResultToToast("resume scheduler failed: " + error.getDescription());
                }
            }
        });
    }

    //Listeners
    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };

    private MediaManager.VideoPlaybackStateListener updatedVideoPlaybackStateListener =
            new MediaManager.VideoPlaybackStateListener() {
                @Override
                public void onUpdate(MediaManager.VideoPlaybackState videoPlaybackState) {
                    updateStatusTextView(videoPlaybackState);
                }
            };

    private void updateStatusTextView(MediaManager.VideoPlaybackState videoPlaybackState) {
        final StringBuffer pushInfo = new StringBuffer();

        addLineToSB(pushInfo, "Video Playback State", null);
        if (videoPlaybackState != null) {
            if (videoPlaybackState.getPlayingMediaFile() != null) {
                addLineToSB(pushInfo, "media index", videoPlaybackState.getPlayingMediaFile().getIndex());
                addLineToSB(pushInfo, "media size", videoPlaybackState.getPlayingMediaFile().getFileSize());
                addLineToSB(pushInfo,
                        "media duration",
                        videoPlaybackState.getPlayingMediaFile().getDurationInSeconds());
                addLineToSB(pushInfo, "media created date", videoPlaybackState.getPlayingMediaFile().getDateCreated());
                addLineToSB(pushInfo,
                        "media orientation",
                        videoPlaybackState.getPlayingMediaFile().getVideoOrientation());
            } else {
                addLineToSB(pushInfo, "media index", "None");
            }
            addLineToSB(pushInfo, "media current position", videoPlaybackState.getPlayingPosition());
            addLineToSB(pushInfo, "media current status", videoPlaybackState.getPlaybackStatus());
            addLineToSB(pushInfo, "media cached percentage", videoPlaybackState.getCachedPercentage());
            addLineToSB(pushInfo, "media cached position", videoPlaybackState.getCachedPosition());
            pushInfo.append("\n");
            setResultToText(pushInfo.toString());
        }
    }

    private void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.
                append((name == null || "".equals(name)) ? "" : name + ": ").
                append(value == null ? "" : value + "").
                append("\n");
    }

    private void downloadFileByIndex(final int index) {
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }

        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                HideDownloadProgressDialog();
                setResultToToast("Download File Failed" + error.getDescription());
                currentProgress = -1;
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentProgress) {
                    mDownloadDialog.setProgress(tmpProgress);
                    currentProgress = tmpProgress;
                }
            }

            @Override
            public void onStart() {
                currentProgress = -1;
                ShowDownloadProgressDialog();
            }

            @Override
            public void onSuccess(String filePath) {
                HideDownloadProgressDialog();
                setResultToToast("Download File Success" + ":" + filePath);
                currentProgress = -1;
                deleteFileByIndex(index);
            }
        });
    }

    private void deleteFileByIndex(final int index) {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
            mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                @Override
                public void onSuccess(List<MediaFile> x, DJICameraError y) {
                    DJILog.e(TAG, "Delete file success");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            MediaFile file = mediaFileList.remove(index);

                            //Reset select view
                            lastClickViewIndex = -1;
                            lastClickView = null;

                            //Update recyclerView
                            mListAdapter.notifyItemRemoved(index);
                        }
                    });
                }

                @Override
                public void onFailure(DJIError error) {
                    setResultToToast("Delete file failed");
                }
            });
        }
    }

    private void playVideo() {
        mDisplayImageView.setVisibility(View.INVISIBLE);
        MediaFile selectedMediaFile = mediaFileList.get(lastClickViewIndex);
        if ((selectedMediaFile.getMediaType() == MediaFile.MediaType.MOV) || (selectedMediaFile.getMediaType() == MediaFile.MediaType.MP4)) {
            mMediaManager.playVideoMediaFile(selectedMediaFile, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (null != error) {
                        setResultToToast("Play Video Failed " + error.getDescription());
                    } else {
                        DJILog.e(TAG, "Play Video Success");
                    }
                }
            });
        }
    }

    private void moveToPosition() {

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt_input_position, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String ms = userInput.getText().toString();
                mMediaManager.moveToPosition(Integer.parseInt(ms),
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                if (null != error) {
                                    setResultToToast("Move to video position failed" + error.getDescription());
                                } else {
                                    DJILog.e(TAG, "Move to video position successfully.");
                                }
                            }
                        });
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_btn: {
                this.finish();
                break;
            }
            case R.id.delete_btn: {
                deleteFileByIndex(lastClickViewIndex);
                break;
            }
            case R.id.reload_btn: {
                getFileList();
                break;
            }
            case R.id.download_btn: {
                downloadFile();

                break;
            }
            case R.id.status_btn: {
                if (mPushDrawerSd.isOpened()) {
                    mPushDrawerSd.animateClose();
                } else {
                    mPushDrawerSd.animateOpen();
                }
                break;
            }
            case R.id.play_btn: {
                playVideo();
                break;
            }
            case R.id.resume_btn: {
                mMediaManager.resume(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Resume Video Failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Resume Video Success");
                        }
                    }
                });
                break;
            }
            case R.id.pause_btn: {
                mMediaManager.pause(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Pause Video Failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Pause Video Success");
                        }
                    }
                });
                break;
            }
            case R.id.stop_btn: {
                mMediaManager.stop(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Stop Video Failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Stop Video Success");
                        }
                    }
                });
                break;
            }
            case R.id.moveTo_btn: {
                moveToPosition();
                break;
            }
            default:
                break;
        }
    }

    private void downloadFile() {
        if (lastClickViewIndex >= 0) {
            downloadFileByIndex(lastClickViewIndex);
        } else {
            setResultToToast("请选中文件再下载");
        }
    }
}
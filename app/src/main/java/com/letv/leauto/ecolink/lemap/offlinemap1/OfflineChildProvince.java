package com.letv.leauto.ecolink.lemap.offlinemap1;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMapException;
import com.amap.api.maps.offlinemap.OfflineMapCity;
import com.amap.api.maps.offlinemap.OfflineMapManager;
import com.amap.api.maps.offlinemap.OfflineMapStatus;
import com.letv.leauto.ecolink.R;
import com.letv.leauto.ecolink.cfg.GlobalCfg;
import com.letv.leauto.ecolink.cfg.SettingCfg;
import com.letv.leauto.ecolink.ui.dialog.NetworkConfirmDialog;
import com.letv.leauto.ecolink.utils.CacheUtils;
import com.letv.leauto.ecolink.utils.NetUtils;
import com.letv.leauto.ecolink.utils.ToastUtil;
import com.letv.leauto.ecolink.utils.Trace;

import java.util.List;

public class OfflineChildProvince implements OnClickListener, OnLongClickListener {
    //解压结束
    private static final int MSG_UNZIP_FINISHED = 8;
    private Context mContext;
    private TextView mOffLineCityName;// 离线包名称

    private TextView mOffLineCitySize;// 离线包大小

    private TextView mDownloadImageWithLine;// 有线框的下载相关Image
    private TextView mDownloadImageNoLine;//无线框的下载相关Image

    private TextView mDownloadProgress;

    private OfflineMapManager amapManager;

    private OfflineMapCity mMapCity;// 离线下载城市

    Dialog dialog;// 长按弹出的对话框

    private boolean mIsDownloading = false;

    private boolean isProvince = false;

    private boolean isDowned = false;
    //是否正在解压
    private boolean isUnZipping = false;
    private LinearLayout mDownloadLayout;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            int completeCode = (Integer) msg.obj;

            switch (msg.what) {

                case OfflineMapStatus.LOADING:
                    displyaLoadingStatus(completeCode);
                    break;
                case OfflineMapStatus.PAUSE:
                    displayPauseStatus(completeCode);
                    break;
                case OfflineMapStatus.STOP:
                    break;
                case OfflineMapStatus.SUCCESS:
                    displaySuccessStatus();
                    break;
                case MSG_UNZIP_FINISHED:
                    displaySuccessStatus();
                    break;
                case OfflineMapStatus.UNZIP:
                    isUnZipping = true;
                    displayUnZIPStatus(completeCode);
                    break;
                case OfflineMapStatus.ERROR:
                    displayExceptionStatus();
                    break;
                case OfflineMapStatus.WAITING:
                    displayWaitingStatus(completeCode);
                    break;
                case OfflineMapStatus.CHECKUPDATES:
                    displayDefault();
                    break;

                case OfflineMapStatus.EXCEPTION_AMAP:
                case OfflineMapStatus.EXCEPTION_NETWORK_LOADING:
                case OfflineMapStatus.EXCEPTION_SDCARD:
                    displayExceptionStatus();
                    break;
                case OfflineMapStatus.NEW_VERSION:
                    displayExceptionStatus();
                    break;
            }
        }

    };


    public void setProvince(boolean isProvince) {
        this.isProvince = isProvince;
    }


    public OfflineChildProvince(Context context, OfflineMapManager offlineMapManager, boolean isDowned) {
        mContext = context;
        initView();
        amapManager = offlineMapManager;
    }


    public View getOffLineChildView() {
        return mOffLineChildView;
    }

    private View mOffLineChildView;

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (GlobalCfg.IS_POTRAIT) {
            mOffLineChildView = inflater.inflate(R.layout.offlinemap_child_p, null);
        } else {
            mOffLineChildView = inflater.inflate(R.layout.offlinemap_child, null);
        }
        mOffLineCityName = (TextView) mOffLineChildView.findViewById(R.id.name);
        mOffLineCitySize = (TextView) mOffLineChildView
                .findViewById(R.id.name_size);
        mDownloadImageWithLine = (TextView) mOffLineChildView
                .findViewById(R.id.download_status_image);
        mDownloadImageNoLine = (TextView) mOffLineChildView.findViewById(R.id.download_status_image_no_line);
        mDownloadProgress = (TextView) mOffLineChildView
                .findViewById(R.id.download_progress_status);

        mOffLineChildView.setOnClickListener(this);
        mOffLineChildView.setOnLongClickListener(this);
        mDownloadLayout= (LinearLayout) mOffLineChildView.findViewById(R.id.download_layout);
    }

    private void showNoLineDownloadImage(boolean isShow) {
        if (isShow) {
            mDownloadImageNoLine.setVisibility(View.VISIBLE);
            mDownloadImageWithLine.setVisibility(View.GONE);
        } else {
            mDownloadImageWithLine.setVisibility(View.VISIBLE);
            mDownloadImageNoLine.setVisibility(View.GONE);
        }
    }

    public void  setDownLoadVisble(boolean visible){
        if (visible){
            mDownloadLayout.setVisibility(View.VISIBLE);
        }else{
            mDownloadLayout.setVisibility(View.GONE);
        }
    }

    public void setOffLineCity(OfflineMapCity mapCity) {
        if (mapCity != null) {

            mMapCity = mapCity;
            mOffLineCityName.setText(mapCity.getCity());
            double size = ((int) (mapCity.getSize() / 1024.0 / 1024.0 * 100)) / 100.0;
            mOffLineCitySize.setText(String.format("%.2f", size) + " M");
            int state = mapCity.getState();
            int completeCode = mapCity.getcompleteCode();
            List<OfflineMapCity> downloadOfflineMapCityList = getOfflineDownloadCityList();

            String currentCityName = mapCity.getCity();
            Trace.Error("##", "当前city : " + currentCityName);

            for (OfflineMapCity city : downloadOfflineMapCityList) {
                String downloadedCityName = city.getCity();
                Trace.Error("##", "已下载的city ： " + downloadedCityName);
                if (currentCityName.equals(downloadedCityName)) {
                    state = OfflineMapStatus.SUCCESS;
                    completeCode = 100;
                }
            }

            notifyViewDisplay(state, completeCode,mIsDownloading);
        }
    }

    private List<OfflineMapCity> getOfflineDownloadCityList() {
        return amapManager.getDownloadOfflineMapCityList();
    }

    /**
     * 更新显示状态 在被点击和下载进度发生改变时会被调用
     *
     * @param status
     * @param completeCode
     * @param isDownloading
     */
    private void notifyViewDisplay(int status, int completeCode,
                                   boolean isDownloading) {
        if (mMapCity != null) {
            mMapCity.setState(status);
            mMapCity.setCompleteCode(completeCode);
        }
        Message msg = handler.obtainMessage();
        msg.what = status;
        msg.obj = completeCode;
        if (isUnZipping && completeCode == 100 && status == 4) {
            msg.what = MSG_UNZIP_FINISHED;
        }
        handler.sendMessage(msg);

    }

    /**
     * 最原始的状态，未下载，显示下载按钮
     */
    private void displayDefault() {
        mDownloadProgress.setVisibility(View.INVISIBLE);
        showNoLineDownloadImage(false);
        mDownloadImageWithLine.setText(R.string.str_download);
    }

    /**
     * 等待中
     *
     * @param completeCode
     */
    private void displayWaitingStatus(int completeCode) {
        mDownloadProgress.setVisibility(View.INVISIBLE);
        showNoLineDownloadImage(true);
        mDownloadImageNoLine.setVisibility(View.VISIBLE);
        mDownloadImageNoLine.setText(R.string.download_waiting);
    }

    /**
     * 下载出现异常
     */
    private void displayExceptionStatus() {
        mDownloadProgress.setVisibility(View.VISIBLE);
        showNoLineDownloadImage(false);
        mDownloadImageWithLine.setText(R.string.download_resume);
        mDownloadProgress.setText(R.string.download_error);
        mDownloadProgress.setTextColor(mContext.getResources().getColor(R.color.map_down_exception));
    }

    /**
     * 显示有更新
     */
    private void displayHasNewVersion() {
        mDownloadProgress.setVisibility(View.VISIBLE);
        showNoLineDownloadImage(false);
        mDownloadImageWithLine.setText(R.string.download_update);
        mDownloadProgress.setText(R.string.download_have_update);
    }

    /**
     * 暂停
     *
     * @param completeCode
     */
    private void displayPauseStatus(int completeCode) {
        if (mMapCity != null) {
            completeCode = mMapCity.getcompleteCode();
        }

        mDownloadProgress.setVisibility(View.VISIBLE);
        showNoLineDownloadImage(false);
        mDownloadImageWithLine.setText(R.string.download_resume);
        mDownloadProgress.setText(completeCode + "%");

    }

    /**
     * 下载成功
     */
    private void displaySuccessStatus() {
        isUnZipping = false;
        mDownloadProgress.setVisibility(View.INVISIBLE);
        showNoLineDownloadImage(true);
        mDownloadImageNoLine.setText(R.string.download_offline);
//        try {
//            amapManager.updateOfflineCityByName(mMapCity.getCity());;
//            //根据判断check是否更新
//            if(OfflineMapFragment.hasnew){
//                Log.i("TAG", "displaySuccessStatus: ds");
//                mDownloadProgress.setText("更 新");
//                mDownloadProgress.setBackgroundResource(R.drawable.offline_map_update);
//                Log.i("TAG", "onClickSSS: "+mMapCity.getCity());
//                mDownloadProgress.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View arg0) {
//////                       amapManager.remove(mMapCity.getCity());//删除
//                        startDownload();//重新下载
//                        Log.i("TAG", "onClick: "+mMapCity.getCity());
//
//                    }
//                });
//
//
//
//            }
//
//
//        } catch (AMapException e) {
//            e.printStackTrace();
//        }


    }

    /**
     * 正在解压
     */
    private void displayUnZIPStatus(int completeCode) {
        mDownloadProgress.setVisibility(View.VISIBLE);
        mDownloadImageWithLine.setVisibility(View.GONE);
        mDownloadImageNoLine.setVisibility(View.GONE);
        mDownloadProgress.setText(mContext.getString(R.string.download_unPackage) + completeCode + "%");
        mDownloadProgress.setTextColor(mContext.getResources().getColor(R.color.map_down_progress));
//        mDownloadProgress.setTextColor(mContext.getResources().getColor(
//                R.color.gray));
    }

    /**
     * @param completeCode
     */
    private void displyaLoadingStatus(int completeCode) {
        // todo
        if (mMapCity == null) {
            return;
        }

        mDownloadProgress.setVisibility(View.VISIBLE);
        mDownloadProgress.setText(mMapCity.getcompleteCode() + "%");
        mDownloadProgress.setTextColor(mContext.getResources().getColor(R.color.map_down_progress));
        showNoLineDownloadImage(false);
        mDownloadImageWithLine.setText(R.string.download_pause);
    }

    private synchronized void pauseDownload() {
        amapManager.pause();
        //暂停下载之后，开始下一个等待中的任务
//        amapManager.restart();
    }

    /**
     * 启动下载任务
     */
    private synchronized boolean startDownload() {
        if (!CacheUtils.getInstance(mContext).getBoolean(SettingCfg.MAP_DOWNLOAD,false)){

            CacheUtils.getInstance(mContext).putBoolean(SettingCfg.MAP_DOWNLOAD,true);
            mContext.sendBroadcast(new Intent(SettingCfg.MAP_DOWNLOAD));
            Trace.Debug("#### send map receiver");
        }


        try {
            if (isProvince) {
                amapManager.downloadByProvinceName(mMapCity.getCity());
            } else {
                amapManager.downloadByCityName(mMapCity.getCity());
            }
            return true;
        } catch (AMapException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ToastUtil.show(mContext, R.string.download_connect_faild);
//            Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void onClick(View view) {

        int completeCode = -1, status = -1;
        if (mMapCity != null) {
            status = mMapCity.getState();
            completeCode = mMapCity.getcompleteCode();

            switch (status) {
                case OfflineMapStatus.UNZIP:
                case OfflineMapStatus.SUCCESS:
                    // 解压中何在成功啥不干
                    break;
                case OfflineMapStatus.LOADING:
                    pauseDownload();
                    // 在下载中的时候点击，表示要暂停
                    displayPauseStatus(completeCode);
                    break;
                case OfflineMapStatus.PAUSE:
                case OfflineMapStatus.CHECKUPDATES:
                case OfflineMapStatus.ERROR:
                case OfflineMapStatus.WAITING:
                default:
                    if (NetUtils.isConnected(mContext)) {
                        if (!NetUtils.isWifi(mContext)) {
                            if (CacheUtils.getInstance(mContext).getBoolean(
                                    SettingCfg.USE_MOBILE_DOWNLOAD_MAP,false)){
                                if (startDownload())
                                    displayWaitingStatus(mMapCity.getcompleteCode());
                                else
                                    displayExceptionStatus();
                            }
                            else {
                                NetworkConfirmDialog dialog = new NetworkConfirmDialog(mContext, R.string.fourg_download_warn, R.string.continue_download, R.string.cancel, true, true);
                                dialog.setListener(new NetworkConfirmDialog.OnClickListener() {
                                    @Override
                                    public void onConfirm(boolean checked) {
                                        CacheUtils.getInstance(mContext).putBoolean(
                                                SettingCfg.USE_MOBILE_DOWNLOAD_MAP,checked);

                                        if (startDownload())
                                            displayWaitingStatus(mMapCity.getcompleteCode());
                                        else
                                            displayExceptionStatus();
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                                dialog.show();
                            }
                        } else {
                            if (startDownload())
                                displayWaitingStatus(completeCode);
                            else
                                displayExceptionStatus();
                        }
                    } else {
                        ToastUtil.show(mContext, R.string.no_net);
                    }
                    // 在暂停中点击，表示要开始下载
                    // 在默认状态点击，表示开始下载
                    // 在等待中点击，表示要开始下载
                    // 要开始下载状态改为等待中，再回调中会自己修改
                    break;
            }
        }

    }

    /**
     * 长按弹出提示框 删除（取消）下载
     * 加入synchronized 避免在dialog还没有关闭的时候再次，请求弹出的bug
     */
    public synchronized void showDeleteDialog(final String name) {
        Builder builder = new Builder(mContext);

        builder.setTitle(name);
        builder.setSingleChoiceItems(new String[]{"删除"}, -1,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        dialog.dismiss();
                        if (amapManager == null) {
                            return;
                        }
                        switch (arg1) {
                            case 0:
                                amapManager.remove(name);
                                break;

                            default:
                                break;
                        }
                    }
                });

        builder.setNegativeButton(mContext.getString(R.string.cancel), null);

        dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        dialog.show();
    }

    /**
     * 长按弹出提示框 删除和更新
     */
    public void showDeleteUpdateDialog(final String name) {
        Builder builder = new Builder(mContext);

        builder.setTitle(name);
        builder.setSingleChoiceItems(new String[]{mContext.getString(R.string.delete), mContext.getString(R.string.download_check_update)}, -1,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        dialog.dismiss();
                        if (amapManager == null) {
                            return;
                        }
                        switch (arg1) {
                            case 0:
                                //TODO:
                                amapManager.remove(name);
                                break;
                            case 1:
                                try {
                                    amapManager.updateOfflineCityByName(name);
                                } catch (AMapException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                break;
                        }

                    }
                });
        builder.setNegativeButton(R.string.cancel, null);
        dialog = builder.create();
        dialog.show();
    }

    public boolean onLongClick(View arg0) {

//        Log.d("amap-longclick",
//                mMapCity.getCity() + " : " + mMapCity.getState());
//        if(isDowned){
//            showDeleteDialog(mMapCity.getCity());
//        }
//       if (mMapCity.getState() == OfflineMapStatus.SUCCESS) {
//            showDeleteUpdateDialog(mMapCity.getCity());
//        } else if (mMapCity.getState() != OfflineMapStatus.CHECKUPDATES) {
//            showDeleteDialog(mMapCity.getCity());
//        }
        return false;
    }


}

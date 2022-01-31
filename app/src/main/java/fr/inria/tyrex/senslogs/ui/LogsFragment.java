package fr.inria.tyrex.senslogs.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.CopyTask;
import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.ZipCreationTask;
import fr.inria.tyrex.senslogs.databinding.FragmentLogsBinding;
import fr.inria.tyrex.senslogs.model.log.Log;
import fr.inria.tyrex.senslogs.ui.dialog.LogDialog;
import fr.inria.tyrex.senslogs.ui.utils.DividerItemDecoration;
import fr.inria.tyrex.senslogs.ui.utils.StringsFormat;

/**
 * This fragment will show the list of sensors logs, then some actions for each log are enabled:
 * share, delete, copy to SD card
 */
public class LogsFragment extends Fragment {

    public static final String INPUT_LOG = "log";
    private static final String BUNDLE_TAG = "logsFragment";

    private static final int REQUEST_CODE_SHARE = 1;

    private RecyclerView mRecyclerView;

    private LogsManager mLogsManager;
    private List<Log> mLogs;

    private MultiSelector mMultiSelector = new MultiSelector();
    private FragmentLogsBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mLogsManager = ((Application) getActivity().getApplication()).getLogsManager();

    }

    @Override
    public void onActivityCreated(Bundle bundle) {

        if (mMultiSelector != null) {

            if (bundle != null) {
                Bundle bundleLog = bundle.getBundle(BUNDLE_TAG);
                if (bundleLog != null)
                    mMultiSelector.restoreSelectionStates(bundleLog);
            }

            if (mMultiSelector.isSelectable()) {
                if (mDeleteMode != null) {
                    mDeleteMode.setClearOnPrepare(false);
                    ((AppCompatActivity) getActivity()).startSupportActionMode(mDeleteMode);
                }

            }
        }

        super.onActivityCreated(bundle);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBundle(BUNDLE_TAG, mMultiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        mBinding = FragmentLogsBinding.inflate(getLayoutInflater());

        View v = mBinding.root;

        mLogs = mLogsManager.getLogs();
        mBinding.setLogs(mLogs);

        Log selectedLog = null;
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(INPUT_LOG)) {
            selectedLog = mLogsManager.getLogs().get(intent.getIntExtra(INPUT_LOG, -1));
        }

        Collections.sort(mLogs, (lhs, rhs) ->
                -Double.compare(lhs.getRecordTimes().startTime, rhs.getRecordTimes().startTime));

        LogsAdapter adapter = new LogsAdapter();

        mRecyclerView = v.findViewById(R.id.logs_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(adapter);


        if (selectedLog != null) selectLog(selectedLog);

        return v;
    }


    private void selectLog(Log log) {

        if (log == null) {
            return;
        }

        LogDialog newFragment = LogDialog.newInstance(log);
        newFragment.show(getActivity().getSupportFragmentManager(), "fragment_log_dialog");
        newFragment.setListener(new LogDialog.OnDialogResultListener() {
            @Override
            public void onDeleteResult(Log log) {
                delete(log);
            }

            @Override
            public void onShareResult(Log log) {
                share(log);
            }

            @Override
            public void onCopyToSdCardResult(final Log log) {
                copy(log);
            }
        });
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.log_list_item_context, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            getActivity().supportFinishAfterTransition();
        }

        return true;
    }


    private File mSharedTmpFile;




    /*
    Actions
     */

    private void delete(Log log) {
        List<Log> tmpList = new ArrayList<>();
        tmpList.add(log);
        delete(tmpList);

        mBinding.setLogs(mLogsManager.getLogs());
    }

    private void delete(List<Log> logs) {

        for (Log log : logs) {
            mRecyclerView.getAdapter().notifyItemRemoved(mLogs.indexOf(log));
            mLogsManager.deleteLog(log);
        }

        String snackBarMessage;
        if (logs.size() > 1) {
            snackBarMessage = String.format(getString(R.string.log_multiple_deleted_snackbar),
                    logs.size());
        } else {
            snackBarMessage = String.format(getString(R.string.log_deleted_snackbar),
                    logs.get(0).getName());
        }

        Snackbar.make(mRecyclerView, Html.fromHtml(snackBarMessage), Snackbar.LENGTH_LONG).show();

        mBinding.setLogs(mLogsManager.getLogs());
    }


    private void share(final Log log) {

        mSharedTmpFile = new File(getContext().getExternalCacheDir(),
                log.getZipFile().getName());

        final ProgressDialog alertDialog = new ProgressDialog(getActivity());
        alertDialog.setCancelable(false);
        alertDialog.setMax((int) log.getZipFile().length());

        CopyTask task = new CopyTask();
        CopyTask.Listener listener = new CopyTask.Listener() {
            @Override
            public void onCopyFinished(File outputFile) {
                alertDialog.dismiss();
            }

            @Override
            public void onProgress(Long currentSize) {
                alertDialog.setProgress(currentSize.intValue());
            }
        };
        task.setListener(listener);
        task.execute(new CopyTask.Input(log.getZipFile(), mSharedTmpFile));


        if(getContext() == null) return;

        Uri zipUri = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".provider",
                mSharedTmpFile);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
        shareIntent.setType("application/zip");
        Intent chooser = Intent.createChooser(shareIntent, getText(R.string.send_to));

        // https://stackoverflow.com/a/59439316/2239938
        List<ResolveInfo> resInfoList = getContext().getPackageManager()
                .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            getContext().grantUriPermission(packageName, zipUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivityForResult(chooser, REQUEST_CODE_SHARE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SHARE && mSharedTmpFile != null) {
            // Following code works only if the file finished to be sent like gmail.
//            if(!mSharedTmpFile.delete()) {
//                android.util.Log.e(Application.LOG_TAG, "Cannot delete log file");
//            }
            mSharedTmpFile = null;
        }
    }


    private void copy(final Log log) {

        final ProgressDialog alertDialog = new ProgressDialog(getActivity());
        alertDialog.setCancelable(false);
        alertDialog.setMax((int) log.getZipFile().length());
        alertDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        alertDialog.setMessage(getString(R.string.log_copy_to_sd_card_progress));

        CopyTask.Listener listener = new CopyTask.Listener() {
            @Override
            public void onCopyFinished(File outputFile) {
                String outputPath = outputFile.getAbsolutePath();
                Snackbar snackbar = Snackbar.make(mRecyclerView,
                        Html.fromHtml(String.format(getString(R.string.log_copy_to_sd_card_snackbar),
                                log.getName(), outputPath)), Snackbar.LENGTH_LONG);
                snackbar.show();
                alertDialog.dismiss();
            }

            @Override
            public void onProgress(Long currentSize) {
                alertDialog.setProgress(currentSize.intValue());
            }
        };

        alertDialog.show();
        mLogsManager.copyLogToSdCard(getActivity(), log, listener);
    }


    private ModalMultiSelectorCallback mDeleteMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.log_list_item_context, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_item_delete_log) {
                actionMode.finish();

                List<Log> itemsClicked = new ArrayList<>();


                for (int i = mLogs.size(); i >= 0; i--) {
                    if (mMultiSelector.isSelected(i, 0)) {
                        itemsClicked.add(mLogs.get(i));
                    }
                }

                delete(itemsClicked);

                mMultiSelector.clearSelections();
                return true;

            }
            return false;
        }
    };


    public class LogHolder extends SwappingHolder implements View.OnClickListener,
            View.OnLongClickListener {

        private TextView mName;
        private TextView mCompressedSize;
        private TextView mDateTime;
        private ImageButton mShare;
        private final ProgressBar mProgressBar;
        private final View mDataContainer;

        private Log mLog;

        public LogHolder(View v) {
            super(v, mMultiSelector);

            mName = v.findViewById(R.id.log_name);
            mCompressedSize = v.findViewById(R.id.log_compressed_size);
            mDateTime = v.findViewById(R.id.log_date_time);
            mShare = v.findViewById(R.id.logs_share_button);
            mProgressBar = v.findViewById(R.id.logs_progress_bar);
            mDataContainer = v.findViewById(R.id.log_data_container);
            mProgressBar.setMax(100);
            v.setOnClickListener(this);
            v.setLongClickable(true);
            v.setOnLongClickListener(this);

        }

        public void bindLog(Log log) {
            mLog = log;

            mName.setText(log.getName());
            mDateTime.setText(DateFormat.getDateTimeInstance().format(log.getRecordTimes().startTime * 1000));
            mCompressedSize.setText(StringsFormat.getSize(getResources(), log.getCompressedSize()));
            mShare.setOnClickListener(v -> share(mLog));

            if (mLog.getCreationTask() != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                mDataContainer.setVisibility(View.GONE);

                mLog.getCreationTask().addListener(new ZipCreationTask.ZipCreationListener() {
                    @Override
                    public void onProgress(File currentFile, float ratio) {
                        mProgressBar.setProgress((int) (100 * ratio));
                    }

                    @Override
                    public void onTaskFinished(File outputFile, long fileSize) {

                        if (!isAdded()) {
                            return;
                        }
                        mProgressBar.setVisibility(View.GONE);
                        mDataContainer.setVisibility(View.VISIBLE);
                        mCompressedSize.setText(StringsFormat.getSize(getResources(), fileSize));
                    }
                });
            }

        }

        @Override
        public void onClick(View v) {

            if (mLog == null) {
                return;
            }
            if (!mMultiSelector.tapSelection(this)) {
                selectLog(mLog);
            }

        }

        @Override
        public boolean onLongClick(View v) {

            ((AppCompatActivity) getActivity()).startSupportActionMode(mDeleteMode);
            mMultiSelector.setSelected(this, true);
            return true;
        }

    }


    public class LogsAdapter extends RecyclerView.Adapter<LogHolder> {

        @Override
        public LogHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_list_item, parent, false);

            return new LogHolder(v);
        }

        @Override
        public void onBindViewHolder(LogHolder holder, int position) {
            holder.bindLog(mLogs.get(position));
        }

        @Override
        public int getItemCount() {
            return mLogs.size();
        }
    }

}


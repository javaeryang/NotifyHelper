package com.fun.notifyhelper;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fun.notifyhelper.adapter.AppAdapter;
import com.fun.notifyhelper.databinding.FragmentAppPickerBinding;
import com.fun.notifyhelper.model.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppPickerFragment extends Fragment {

    public static final String REQUEST_KEY_SELECT_APP = "request_key_select_app";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_APP_NAME = "extra_app_name";

    private FragmentAppPickerBinding binding;
    private AppAdapter adapter;
    private final List<AppInfo> allApps = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new AppAdapter();
        binding.rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvApps.setAdapter(adapter);

        adapter.setOnAppClickListener(appInfo -> {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_PACKAGE_NAME, appInfo.getPackageName());
            bundle.putString(EXTRA_APP_NAME, appInfo.getAppName());
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_SELECT_APP, bundle);
            NavHostFragment.findNavController(AppPickerFragment.this).navigateUp();
        });

        binding.searchViewApp.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterApps(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });

        loadInstalledApps();
    }

    private void loadInstalledApps() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvApps.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            PackageManager pm = requireContext().getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
            List<AppInfo> apps = new ArrayList<>();

            for (ResolveInfo info : resolveInfos) {
                if (info.activityInfo == null || info.activityInfo.packageName == null) continue;

                // Don't list our own app
                if (info.activityInfo.packageName.equals(requireContext().getPackageName())) {
                    continue;
                }

                String appName = info.loadLabel(pm).toString();
                String packageName = info.activityInfo.packageName;
                Drawable icon = info.loadIcon(pm);

                apps.add(new AppInfo(appName, packageName, icon));
            }

            Collections.sort(apps, (o1, o2) -> o1.getAppName().compareToIgnoreCase(o2.getAppName()));

            mainHandler.post(() -> {
                if (!isAdded() || binding == null) return;
                allApps.clear();
                allApps.addAll(apps);

                binding.progressBar.setVisibility(View.GONE);
                if (allApps.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.rvApps.setVisibility(View.GONE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                    binding.rvApps.setVisibility(View.VISIBLE);
                    adapter.setApps(allApps);
                }
            });
        });
    }

    private void filterApps(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setApps(allApps);
            binding.tvEmpty.setVisibility(allApps.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (app.getAppName().toLowerCase().contains(lowerQuery) ||
                app.getPackageName().toLowerCase().contains(lowerQuery)) {
                filtered.add(app);
            }
        }

        adapter.setApps(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

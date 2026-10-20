package com.fun.notifyhelper;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fun.notifyhelper.adapter.AppAdapter;
import com.fun.notifyhelper.databinding.FragmentAppPickerBinding;
import com.fun.notifyhelper.model.AppInfo;
import com.fun.notifyhelper.model.TargetApp;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppPickerFragment extends Fragment {

    public static final String REQUEST_KEY_SELECT_APP = "request_key_select_app";
    public static final String EXTRA_TARGET_APPS_JSON = "extra_target_apps_json";

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

        adapter.setOnAppClickListener((appInfo, position) -> updateFab());

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

        binding.fabConfirmSelection.setOnClickListener(v -> confirmSelection());

        loadInstalledApps();
    }

    private void updateFab() {
        List<AppInfo> selected = adapter.getSelectedApps();
        if (selected.isEmpty()) {
            binding.fabConfirmSelection.setVisibility(View.GONE);
        } else {
            binding.fabConfirmSelection.setVisibility(View.VISIBLE);
            binding.fabConfirmSelection.setText("确认选择 (" + selected.size() + ")");
        }
    }

    private void confirmSelection() {
        List<AppInfo> selected = adapter.getSelectedApps();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "请至少选择一个应用", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (AppInfo info : selected) {
            TargetApp targetApp = new TargetApp(
                    info.getAppName(),
                    info.getPackageName(),
                    info.getComponentName(),
                    info.getUserSerialNumber()
            );
            try {
                jsonArray.put(targetApp.toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TARGET_APPS_JSON, jsonArray.toString());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY_SELECT_APP, bundle);
        NavHostFragment.findNavController(AppPickerFragment.this).navigateUp();
    }

    private void loadInstalledApps() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvApps.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            List<AppInfo> apps = new ArrayList<>();
            Context context = requireContext();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

                if (launcherApps != null && userManager != null) {
                    List<UserHandle> userProfiles = userManager.getUserProfiles();
                    for (UserHandle profile : userProfiles) {
                        long serialNumber = userManager.getSerialNumberForUser(profile);
                        List<LauncherActivityInfo> activityList = launcherApps.getActivityList(null, profile);
                        for (LauncherActivityInfo info : activityList) {
                            if (info.getComponentName() != null) {
                                String pkgName = info.getComponentName().getPackageName();
                                if (pkgName.equals(context.getPackageName())) {
                                    continue;
                                }
                                String appName = info.getLabel().toString();
                                String compName = info.getComponentName().flattenToString();
                                android.graphics.drawable.Drawable icon = info.getIcon(0);

                                apps.add(new AppInfo(appName, pkgName, compName, icon, serialNumber));
                            }
                        }
                    }
                }
            } else {
                // Fallback for pre-Lollipop (no dual apps support natively via LauncherApps easily)
                android.content.pm.PackageManager pm = context.getPackageManager();
                android.content.Intent mainIntent = new android.content.Intent(android.content.Intent.ACTION_MAIN, null);
                mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER);

                List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
                for (android.content.pm.ResolveInfo info : resolveInfos) {
                    if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
                    if (info.activityInfo.packageName.equals(context.getPackageName())) {
                        continue;
                    }
                    String appName = info.loadLabel(pm).toString();
                    String packageName = info.activityInfo.packageName;
                    String componentName = new android.content.ComponentName(packageName, info.activityInfo.name).flattenToString();
                    android.graphics.drawable.Drawable icon = info.loadIcon(pm);

                    apps.add(new AppInfo(appName, packageName, componentName, icon, 0));
                }
            }

            Collections.sort(apps, (o1, o2) -> {
                int nameCompare = o1.getAppName().compareToIgnoreCase(o2.getAppName());
                if (nameCompare == 0) {
                    return Long.compare(o1.getUserSerialNumber(), o2.getUserSerialNumber());
                }
                return nameCompare;
            });

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
            updateFab();
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
        updateFab();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

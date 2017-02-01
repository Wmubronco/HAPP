package com.hypodiabetic.happplus.plugins.AbstractClasses;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hypodiabetic.happplus.Intents;
import com.hypodiabetic.happplus.MainApp;
import com.hypodiabetic.happplus.R;
import com.hypodiabetic.happplus.helperObjects.DeviceStatus;
import com.hypodiabetic.happplus.helperObjects.PluginPref;
import com.hypodiabetic.happplus.helperObjects.SysPref;
import com.hypodiabetic.happplus.plugins.devices.SysProfileDevice;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import layout.PrefPopupWindow;


/**
 * Created by Tim on 25/12/2016.
 * Base plugin object, this object is extended to a plugin type and then the plugin itself
 * example: PluginBase > PluginBaseCGM > xDripCGM
 */

public abstract class AbstractPluginBase extends Fragment {

    final public String TAG;
    final protected Context context;
    final protected String PREF_PREFIX;

    private boolean isLoaded;
    private List<SysPref> pluginPrefs;

    public static final int PLUGIN_TYPE_SOURCE  =   1;
    public static final int PLUGIN_TYPE_DEVICE  =   2;
    public static final int PLUGIN_TYPE_SYNC    =   3;

    public static final int DATA_TYPE_CGM       =   1;
    public static final int DATA_TYPE_APS       =   2;

    private static final String PREF_ENABLED    =   "enabled";

    public AbstractPluginBase(){
        this.TAG                =   getTagName();
        this.context            =   MainApp.getInstance();
        this.PREF_PREFIX        =   TAG + ":";
    }

    private String getTagName(){
        String type;
        switch (getPluginType()){
            case PLUGIN_TYPE_SOURCE:
                type    =   "SOURCE";
                break;
            case PLUGIN_TYPE_DEVICE:
                type    =   "DEVICE";
                break;
            case PLUGIN_TYPE_SYNC:
                type    =   "SYNC";
                break;
            default:
                type    =   "unknown Plugin Type";
        }
        return "Plugin:" + type + ":" + getPluginName();
    }

    /**
     * Sets if the plugin is enabled, this value is ignored for Device Plugins that always load
     * @param enabled boolean
     */
    public void setEnabled(Boolean enabled){
        savePref(PREF_ENABLED, enabled);
    }

    /**
     * Any code required to load the plugin
     * @return if load was successful or not
     */
    protected abstract boolean onLoad();
    public boolean load(){
        loadPrefs();
        if (getPluginType() == PLUGIN_TYPE_DEVICE) {        //always load Device Plugins
            if (onLoad()) {
                isLoaded = true;
                Log.d(TAG, "load: Successful");
            } else {
                isLoaded = false;
                Log.e(TAG, "load: Unsuccessful, " + getStatus().getComment());
            }
        } else {
            if (getPref(PREF_ENABLED).getBooleanValue()) {
                if (onLoad()) {
                    isLoaded = true;
                    Log.d(TAG, "load: Successful");
                } else {
                    isLoaded = false;
                    Log.e(TAG, "load: Unsuccessful, " + getStatus().getComment());
                }
            } else {
                isLoaded = false;
                Log.e(TAG, "load: Unsuccessful, plugin is not enabled");
            }
        }
        return isLoaded;
    }

    /**
     * Any code required to unload the plugin
     * @return if unload was successful or not
     */
    protected abstract boolean onUnLoad();
    public boolean unLoad(){
        if (onUnLoad()){
            isLoaded = false;
            Log.d(TAG, "unLoad: Successful");

        } else {
            isLoaded = true;
            Log.e(TAG, "unLoad: Unsuccessful");
        }
        return isLoaded;
    }

    /**
     * Reloads prefs and notifies the plugin
     */
    public void refreshPrefs(){
        loadPrefs();
        onPrefChange(null);
    }
    /**
     * Reloads prefs, notifies the plugin and sends the refreshed SysPref object
     */
    public void refreshPrefs(SysPref sysPref){
        loadPrefs();
        onPrefChange(sysPref);
    }

    /**
     * Called when a Pref for this plugin has been updated or when refreshPrefs() is called
     * A SysPref object is passed if a pref was updated
     */
    protected abstract void onPrefChange(SysPref sysPref);

    /**
     * Names of Prefs for this Plugin, if any
     * @return String List of Pref Names
     */
    protected abstract List<PluginPref> getPrefsList();
    protected void loadPrefs(){
        Log.i(TAG, "loadPrefs: for " + TAG + " Started");
        pluginPrefs =   new ArrayList<>();
        pluginPrefs.add(new SysPref<>(PREF_PREFIX + PREF_ENABLED, PREF_ENABLED, context.getString(R.string.pref_enabled_desc), Arrays.asList("true", "false"), Arrays.asList("true", "false"), PluginPref.PREF_TYPE_LIST));
        for (PluginPref pref : getPrefsList()) {
            SysPref sysPref =   new SysPref<>(PREF_PREFIX + pref.getName(), pref.getDisplayName(), pref.getDescription(), pref.getValues(), pref.getDisplayValues(), pref.getPrefType());
            pluginPrefs.add(sysPref);
            Log.i(TAG, sysPref.getPrefName() + "=" + sysPref.getStringValue() + " | " + sysPref.getPrefDescription());
        }
        Log.i(TAG, "loadPrefs: for " + TAG + " Completed");
    }

    /**
     * Gets a pref for this Plugin
     * @param prefName name of pref
     * @return SysPref object
     */
    protected SysPref getPref(String prefName){
        for (SysPref pref : pluginPrefs){
            if (pref.getPrefName().equals(PREF_PREFIX + prefName)) return pref;
        }
        return null;
    }

    /**
     * Saves a pref for this plugin, its TAG is prefixed to be sure pref does not clash with other plugins
     * @param prefName the name of the pref
     * @param prefValue value of pref
     */
    protected void savePref(String prefName, String prefValue){
        savePluginPref(PREF_PREFIX + prefName, prefValue);
    }
    protected void savePref(String prefName, Integer prefValue){
        savePluginPref(PREF_PREFIX + prefName, prefValue.toString());
    }
    protected void savePref(String prefName, Double prefValue){
        savePluginPref(PREF_PREFIX + prefName, prefValue.toString());
    }
    protected void savePref(String prefName, Boolean prefValue){
        savePluginPref(PREF_PREFIX + prefName, prefValue.toString());
    }
    protected void savePluginPref(String prefName, String prefValue){
        SysProfileDevice deviceSysProfile = (SysProfileDevice) MainApp.getPluginByClass(SysProfileDevice.class);
        deviceSysProfile.savePref(prefName, prefValue);
        loadPrefs();

        Intent prefUpdate = new Intent(Intents.newLocalEvent.NEW_LOCAL_EVENT_PREF_UPDATE);
        prefUpdate.putExtra(Intents.extras.PLUGIN_NAME, getPluginName());
        prefUpdate.putExtra(Intents.extras.PLUGIN_TYPE, getPluginType());
        prefUpdate.putExtra(Intents.extras.PLUGIN_PREF_NAME, prefName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(prefUpdate);
    }

    /**
     * Setups a Plugin Prefs UI Layout for the user to view and change Prefs value
     * @param linearLayout layout that has pref_layout.xml included
     * @param rootView view of parent plugin
     * @param sysPref the sysPref we are displaying
     */
    protected void setPluginPref(LinearLayout linearLayout, View rootView, SysPref sysPref){
        final TextView prefValue        =   (TextView) linearLayout.findViewById(R.id.prefValue);
        TextView prefTitle              =   (TextView) linearLayout.findViewById(R.id.prefTitle);
        RelativeLayout prefValueLayout  =   (RelativeLayout) linearLayout.findViewById(R.id.prefValueLayout);
        prefTitle.setText(              sysPref.getPrefDisplayName());
        prefValue.setText(              sysPref.getPrefDisplayValue());
        final PrefPopupWindow popupWindow = new PrefPopupWindow(rootView.getContext(), sysPref, this, prefValue);
        prefValueLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popupWindow.show(v.getRootView(), 0, -250);
            }
        });
    }

    /**
     * Number of prefs this plugin has
     * @return count of prefs
     */
    public Integer getPrefCount(){
        //Remove one for the default pref "enabled"
        return pluginPrefs.size() - 1;
    }

    /**
     * Current Status of the plugin
     * @return DeviceStatus object
     */
    public DeviceStatus getStatus(){
        DeviceStatus deviceStatus = getPluginStatus();

        //Any prefs missing?
        String prefCheck = checkPrefs();
        if (!prefCheck.equals("")){
            deviceStatus.hasError(true);
            deviceStatus.addComment(prefCheck);
        }
        //enabled
        if (getPluginType() != PLUGIN_TYPE_DEVICE) {
            if (!getPref(PREF_ENABLED).getBooleanValue()) {
                deviceStatus.hasError(true);
                deviceStatus.addComment(context.getString(R.string.plugin_not_enabled));
            }
        }

        return deviceStatus;
    }

    /**
     * Checks that all prefs for this plugin have been set
     * @return details of missing prefs
     */
    private String checkPrefs(){
        String summary = "";
        if (pluginPrefs != null) {
            for (SysPref pref : pluginPrefs){
                if (pref.getStringValue() == null && !pref.getPrefName().equals(TAG + ":" + PREF_ENABLED)) summary += context.getString(R.string.pref) + " '" + pref.getPrefName() + "' " + context.getString(R.string.pref_missing) + ". ";
            }
        }
        return summary;
    }

    /**
     * Checks to see if this Device has any errors or warnings
     * If enabled and Prefs for this plugin are already checked for any null values, error if so
     * @return DeviceStatus object with details
     */
    protected abstract DeviceStatus getPluginStatus();

    /**
     * Is this plugin loaded?
     * @return boolean
     */
    public boolean getIsLoaded(){
        return isLoaded;
    }

    /**
     * Used for display in lists, etc
     * @return Display name of plugin
     */
    public String toString(){
        return getPluginDisplayName();
    }

    /**
     * System name of this plugin
     * @return string
     */
    public abstract String getPluginName();

    /**
     * Display friendly name for this plugin
     * @return string
     */
    public abstract String getPluginDisplayName();

    /**
     * Display friendly description of this plugin
     * @return string
     */
    public abstract String getPluginDescription();

    /**
     * Type of plugin this is
     * @return int as listed in PluginBase, example PLUGIN_TYPE_SOURCE
     */
    public abstract int getPluginType();

    /**
     * Type of data this plugin handel's
     * @return int as listed in PluginBase, example DATA_TYPE_CGM
     */
    //public abstract int getPluginDataType();

    /**
     * Does this plugin load on app start?
     * @return boolean
     */
    public abstract boolean getLoadInBackground();

    /**
     * Detailed list of current values and condition of the plugin for Debug
     * @return JSONArray of details
     */
    public abstract JSONArray getDebug();

}
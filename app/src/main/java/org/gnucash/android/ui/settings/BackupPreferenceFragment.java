/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.ui.settings.dialog.OwnCloudDialogFragment;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Fragment for displaying general preferences
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class BackupPreferenceFragment extends PreferenceFragmentCompat implements
		Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

	/**
	 * Collects references to the UI elements and binds click listeners
	 */
	private static final int REQUEST_LINK_TO_DBX = 0x11;
	public static final int REQUEST_RESOLVE_CONNECTION = 0x12;

	/**
	 * String for tagging log statements
	 */
	public static final String LOG_TAG = "BackupPrefFragment";


	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.fragment_backup_preferences);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_backup_prefs);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


		String keyDefaultEmail = getString(R.string.key_default_export_email);		
		Preference pref = findPreference(keyDefaultEmail);
		String defaultEmail = sharedPrefs.getString(keyDefaultEmail, null);
		if (defaultEmail != null && !defaultEmail.trim().isEmpty()){
			pref.setSummary(defaultEmail);			
		}
		pref.setOnPreferenceChangeListener(this);

        String keyDefaultExportFormat = getString(R.string.key_default_export_format);
        pref = findPreference(keyDefaultExportFormat);
        String defaultExportFormat = sharedPrefs.getString(keyDefaultExportFormat, null);
        if (defaultExportFormat != null && !defaultExportFormat.trim().isEmpty()){
            pref.setSummary(defaultExportFormat);
        }
        pref.setOnPreferenceChangeListener(this);

		pref = findPreference(getString(R.string.key_restore_backup));
		pref.setOnPreferenceClickListener(this);

		pref = findPreference(getString(R.string.key_create_backup));
		pref.setOnPreferenceClickListener(this);


		pref = findPreference(getString(R.string.key_owncloud_sync));
		pref.setOnPreferenceClickListener(this);
		toggleOwnCloudPreference(pref);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		if (key.equals(getString(R.string.key_restore_backup))){
			restoreBackup();
		}

		if (key.equals(getString(R.string.key_owncloud_sync))){
			toggleOwnCloudSync(preference);
			toggleOwnCloudPreference(preference);
		}

		if (key.equals(getString(R.string.key_create_backup))){
			boolean result = GncXmlExporter.createBackup();
			int msg = result ? R.string.toast_backup_successful : R.string.toast_backup_failed;
			Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	/**
     * Listens for changes to the preference and sets the preference summary to the new value
     * @param preference Preference which has been changed
     * @param newValue New value for the changed preference
     * @return <code>true</code> if handled, <code>false</code> otherwise
     */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
		if (preference.getKey().equals(getString(R.string.key_default_currency))){
			GnuCashApplication.setDefaultCurrencyCode(newValue.toString());
		}
		
		if (preference.getKey().equals(getString(R.string.key_default_export_email))){
			String emailSetting = newValue.toString();
			if (emailSetting == null || emailSetting.trim().isEmpty()){
				preference.setSummary(R.string.summary_default_export_email);
			}					
		}

        if (preference.getKey().equals(getString(R.string.key_default_export_format))){
            String exportFormat = newValue.toString();
            if (exportFormat == null || exportFormat.trim().isEmpty()){
                preference.setSummary(R.string.summary_default_export_format);
            }
        }
		return true;
	}



	/**
	 * Toggles the checkbox of the ownCloud Sync preference if an ownCloud account is linked
	 * @param pref ownCloud Sync preference
	 */
	public void toggleOwnCloudPreference(Preference pref) {
		SharedPreferences mPrefs = getActivity().getSharedPreferences(getString(R.string.owncloud_pref), Context.MODE_PRIVATE);
		((CheckBoxPreference)pref).setChecked(mPrefs.getBoolean(getString(R.string.owncloud_sync), false));
	}

	/**
	 * Toggles synchronization with ownCloud on or off
	 */
	private void toggleOwnCloudSync(Preference pref){
		SharedPreferences mPrefs = getActivity().getSharedPreferences(getString(R.string.owncloud_pref), Context.MODE_PRIVATE);

		if (mPrefs.getBoolean(getString(R.string.owncloud_sync), false))
			mPrefs.edit().putBoolean(getString(R.string.owncloud_sync), false).apply();
		else {
			OwnCloudDialogFragment ocDialog = OwnCloudDialogFragment.newInstance(pref);
            ocDialog.show(getActivity().getSupportFragmentManager(), "owncloud_dialog");
		}
	}
	/**
	 * Opens a dialog for a user to select a backup to restore and then restores the backup
	 */
	private void restoreBackup() {
		Log.i("Settings", "Opening GnuCash XML backups for restore");
		String bookUID = BooksDbAdapter.getInstance().getActiveBookUID();
		File[] backupFiles = new File(Exporter.getBackupFolderPath(bookUID)).listFiles();
		if (backupFiles == null || backupFiles.length == 0){
			android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity())
					.setTitle("No backups found")
					.setMessage("There are no existing backup files to restore from")
					.setNegativeButton(R.string.label_dismiss, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.create().show();
			return;
		}

		Arrays.sort(backupFiles);
		List<File> backupFilesList = Arrays.asList(backupFiles);
		Collections.reverse(backupFilesList);
		final File[] sortedBackupFiles = (File[]) backupFilesList.toArray();

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_singlechoice);
		final DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance();
		for (File backupFile : sortedBackupFiles) {
			long time = Exporter.getExportTime(backupFile.getName());
			if (time > 0)
				arrayAdapter.add(dateFormatter.format(new Date(time)));
			else //if no timestamp was found in the filename, just use the name
				arrayAdapter.add(backupFile.getName());
		}

		AlertDialog.Builder restoreDialogBuilder =  new AlertDialog.Builder(getActivity());
		restoreDialogBuilder.setTitle(R.string.title_select_backup_to_restore);
		restoreDialogBuilder.setNegativeButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		restoreDialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File backupFile = sortedBackupFiles[which];
				new ImportAsyncTask(getActivity()).execute(Uri.fromFile(backupFile));
			}
		});

		restoreDialogBuilder.create().show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode){
		}
	}
}

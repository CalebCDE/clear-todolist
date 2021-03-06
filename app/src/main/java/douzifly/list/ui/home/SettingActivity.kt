package douzifly.list.ui.home

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jakewharton.processphoenix.ProcessPhoenix
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import douzifly.list.R
import douzifly.list.backup.Backup
import douzifly.list.backup.BackupHelper
import douzifly.list.model.ThingsManager
import douzifly.list.settings.Settings
import douzifly.list.settings.Theme
import douzifly.list.utils.*
import douzifly.list.widget.TitleLayout
import java.io.File

/**
 * Created by air on 15/10/18.
 */
class SettingActivity : AppCompatActivity() {

    companion object {
        val RESULT_THEME_CHANGED = 1002
    }

    val inputPanel: TitleLayout by lazy {
        findViewById(R.id.header) as TitleLayout
    }

    val imgThemeColorSelected: ImageView by lazy {
        findViewById(R.id.img_color_selected) as ImageView
    }

    val imgThemeSimpleSelected: ImageView by lazy {
        findViewById(R.id.img_simple_selected) as ImageView
    }

    val txtSoundOnOff: TextView by lazy {
        findViewById(R.id.txt_sound_on_off) as TextView
    }

    val txtVersion: TextView by lazy {
        findViewById(R.id.txt_version) as TextView
    }

    val txtEditGroup: TextView by lazy {
        findViewById(R.id.txt_edit_group) as TextView
    }

    val txtGroup: TextView by lazy {
        findViewById(R.id.txt_group) as TextView
    }

    val txtBackup: TextView by lazy {
        findViewById(R.id.txt_backup_title) as TextView
    }

    val initTheme: Theme = Settings.theme

    fun updateGroupName() {
        txtGroup.text = ThingsManager.currentGroup?.title ?: ""
    }

    val dataListener: () -> Unit = {
        ui {
            updateGroupName()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.home_setting_activity)

        inputPanel.title = resources.getString(R.string.setting)
        inputPanel.txtCount.visibility = View.GONE

        (findViewById(R.id.txt_copyright) as TextView).typeface = fontRailway
        (findViewById(R.id.txt_theme_title) as TextView).typeface = fontRailway
        txtSoundOnOff.typeface = fontRailway
        (findViewById(R.id.txt_sound_title) as TextView).typeface = fontRailway
        txtVersion.typeface = fontRailway
        (findViewById(R.id.txt_version_title) as TextView).typeface = fontRailway
        txtBackup.typeface = fontRailway

        txtBackup.setOnClickListener {
            onBackupClick()
        }

        imgThemeColorSelected.setImageDrawable(
                GoogleMaterial.Icon.gmd_done.colorResOf(R.color.yellowPrimary)
        )

        imgThemeSimpleSelected.setImageDrawable(
                GoogleMaterial.Icon.gmd_done.colorResOf(R.color.yellowPrimary)
        )

        updateThemeSelected()
        updateSoundOnOff()

        findViewById(R.id.theme_simple_container).setOnClickListener {
            onThemeClick(Theme.Dot)
        }

        findViewById(R.id.theme_color_container).setOnClickListener {
            onThemeClick(Theme.Colorful)
        }

        txtSoundOnOff.setOnClickListener {
            onSoundsClick()
        }

        findViewById(R.id.txt_sound_title).setOnClickListener {
            onSoundsClick()
        }

        txtVersion.text = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).versionName

        updateGroupName()

        txtEditGroup.setOnClickListener {
            startActivityForResult(Intent(this, GroupEditorActivity::class.java), 0)
        }

        txtGroup.setOnClickListener {
            startActivityForResult(Intent(this, GroupEditorActivity::class.java), 0)
        }

        ThingsManager.addListener(dataListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        ThingsManager.removeListener(dataListener)
    }

    fun onSoundsClick() {
        Settings.sounds = !Settings.sounds
        updateSoundOnOff()
    }

    fun onBackupClick() {
        AlertDialog.Builder(this).setTitle(R.string.setting_backup)
                .setNegativeButton(R.string.backup) { dialogInterface: DialogInterface, i: Int ->
                    doBackup()
                }.setPositiveButton(R.string.restore) { dialogInterface: DialogInterface, i: Int ->
            doRestore()
        }.create().show()
    }

    fun doBackup() {
        val pd = showProgressDialog(R.string.backuping.toResString(this))
        bg {
            val ret = BackupHelper.backup("list.db")
            ui {
                ui(500) {
                    pd.dismiss()
                }
                if (ret.isEmpty()) {
                    R.string.backup_failed.toResString(this).toast(this)
                } else {
                    R.string.backup_success.toResString(this).toast(this)
                }
            }
        }
    }

    fun restart() {
        ProcessPhoenix.triggerRebirth(this);
    }

    fun showBackupListDailog(adapter: ArrayAdapter<Backup>) {
        AlertDialog.Builder(this)
                .setTitle(R.string.restore)
                .setAdapter(adapter) {
                    dialog: DialogInterface, position: Int ->
                    dialog.dismiss()
                    showConfirmDialog(adapter.getItem(position).path)
                }.show()
    }

    fun showConfirmDialog(path: String) {
        AlertDialog.Builder(this)
                .setMessage(R.string.restore_tip)
                .setPositiveButton(R.string.confirm) {
                    tipDialog: DialogInterface, which: Int ->
                    bg {
                        val ret = BackupHelper.resotre(File(path), "list.db")
                        ui {
                            if (ret) {
                                R.string.restore_success.toResString(this).toast(this)
                                restart()
                            } else {
                                R.string.restore_failed.toResString(this).toast(this)
                            }
                            tipDialog.dismiss()
                        }
                    }

                }.setNegativeButton(R.string.delete) {
            tipDialog: DialogInterface, which: Int ->
            tipDialog.dismiss()
            showProgressDialogAndHide("", 500)
            File(path).delete()

        }.show()
    }

    fun doRestore() {
        val pd = showProgressDialog("")

        bg {
            val backups = BackupHelper.listBackupFiles()
            ui (500) {
                pd.dismiss()
            }
            if (backups.size > 0) {
                val adapter = ArrayAdapter<Backup>(this, android.R.layout.select_dialog_item)
                adapter.addAll(backups)
                ui {
                    showBackupListDailog(adapter)
                }
            } else {
                ui {
                    R.string.restore_empty.toResString(this).toast(this)
                }
            }
        }

    }

    fun onThemeClick(theme: Theme) {
        if (theme == Settings.theme) {
            // not changed
            return
        }

        Settings.theme = theme
        updateThemeSelected()

        if (initTheme != theme) {
            setResult(RESULT_THEME_CHANGED)
        }

    }

    fun updateThemeSelected() {
        if (Settings.theme == Theme.Colorful) {
            imgThemeColorSelected.visibility = View.VISIBLE
            imgThemeSimpleSelected.visibility = View.GONE
        } else {
            imgThemeColorSelected.visibility = View.GONE
            imgThemeSimpleSelected.visibility = View.VISIBLE
        }
    }

    fun updateSoundOnOff() {
        txtSoundOnOff.text = if (Settings.sounds) resources.getString(R.string.on) else resources.getString(R.string.off)
    }

}
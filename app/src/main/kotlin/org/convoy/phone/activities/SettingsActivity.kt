package org.convoy.phone.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.fossify.commons.activities.ManageBlockedNumbersActivity
import org.fossify.commons.dialogs.ChangeDateTimeFormatDialog
import org.fossify.commons.dialogs.FeatureLockedDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.addLockedLabelIfNeeded
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getFontSizeText
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.isOrWasThankYouInstalled
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.FONT_SIZE_EXTRA_LARGE
import org.fossify.commons.helpers.FONT_SIZE_LARGE
import org.fossify.commons.helpers.FONT_SIZE_MEDIUM
import org.fossify.commons.helpers.FONT_SIZE_SMALL
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ON_CLICK_CALL_CONTACT
import org.fossify.commons.helpers.ON_CLICK_VIEW_CONTACT
import org.fossify.commons.helpers.TAB_CALL_HISTORY
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_LAST_USED
import org.fossify.commons.helpers.isNougatPlus
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.RadioItem
import org.convoy.phone.R
import org.convoy.phone.databinding.ActivitySettingsBinding
import org.convoy.phone.extensions.canLaunchAccountsConfiguration
import org.convoy.phone.extensions.config
import org.convoy.phone.extensions.launchAccountsConfiguration
import org.convoy.phone.helpers.RecentsHelper
import org.convoy.phone.helpers.RECORDING_SOURCE_DEVICE
import org.convoy.phone.helpers.RECORDING_SOURCE_ENVIRONMENT
import org.convoy.phone.models.RecentCall
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    companion object {
    }

    private val binding by viewBinding(ActivitySettingsBinding::inflate)
    private val requestRecordAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            config.callRecordingEnabled = false
            binding.settingsCallRecording.isChecked = false
            toast(R.string.record_audio_permission_required)
        }
    }
    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            config.callRecordingFolderUri = uri.toString()
            updateRecordingFolder()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        refreshMenuItems()

        binding.apply {
            setupEdgeToEdge(padBottomSystem = listOf(settingsNestedScrollview))
            setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
            applyMinimalLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupManageBlockedNumbers()
        setupOnContactClick()
        setupGroupSubsequentCalls()
        setupStartNameWithSurname()
        setupFormatPhoneNumbers()
        setupShowCallConfirmation()
        setupDisableProximitySensor()
        setupDisableSwipeToAnswer()
        setupAlwaysShowFullscreen()
        setupCallRecording()
        setupRecordingSource()
        setupRecordingFolder()
        updateTextColors(binding.settingsHolder)

        binding.apply {
            arrayOf(
                settingsGeneralSettingsLabel,
                settingsStartupLabel,
                settingsCallsLabel,
                settingsRecordingSectionLabel
            ).forEach {
                it.setTextColor(getProperPrimaryColor())
            }
        }
    }

    private fun applyMinimalLayout() = binding.apply {
        settingsColorCustomizationSectionLabel.beGone()
        settingsColorCustomizationHolder.beGone()
        settingsColorCustomizationDivider.beGone()
        settingsPurchaseThankYouHolder.beGone()
        settingsUseEnglishHolder.beGone()
        settingsLanguageHolder.beGone()
        settingsChangeDateTimeFormatHolder.beGone()
        settingsFontSizeHolder.beGone()
        settingsManageTabsHolder.beGone()
        settingsDefaultTabHolder.beGone()
        settingsOpenDialpadAtLaunchHolder.beGone()
        settingsManageSpeedDialHolder.beGone()
        settingsDialpadDivider.beGone()
        settingsDialpadSectionLabel.beGone()
        settingsHideDialpadNumbersHolder.beGone()
        settingsDialpadVibrationHolder.beGone()
        settingsDialpadBeepsHolder.beGone()
        settingsMigrationDivider.beGone()
        settingsMigrationSectionLabel.beGone()
        settingsExportCallsHolder.beGone()
        settingsImportCallsHolder.beGone()
    }

    private fun setupCallRecording() {
        binding.settingsCallRecording.isChecked = config.callRecordingEnabled
        binding.settingsCallRecordingHolder.setOnClickListener {
            binding.settingsCallRecording.toggle()
        }
        binding.settingsCallRecording.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestRecordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            } else {
                config.callRecordingEnabled = isChecked
            }
        }
    }

    private fun setupRecordingSource() {
        updateRecordingSource()
        binding.settingsRecordingSourceHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(1, getString(R.string.record_environment)),
                RadioItem(2, getString(R.string.record_device))
            )
            val checkedId = if (config.callRecordingSource == RECORDING_SOURCE_DEVICE) 2 else 1
            RadioGroupDialog(this, items, checkedId) {
                config.callRecordingSource = if ((it as Int) == 2) RECORDING_SOURCE_DEVICE else RECORDING_SOURCE_ENVIRONMENT
                updateRecordingSource()
            }
        }
    }

    private fun setupRecordingFolder() {
        updateRecordingFolder()
        binding.settingsRecordingFolderHolder.setOnClickListener {
            pickFolder.launch(Uri.EMPTY)
        }
    }

    private fun updateRecordingSource() {
        binding.settingsRecordingSource.text = getString(
            if (config.callRecordingSource == RECORDING_SOURCE_DEVICE) R.string.record_device else R.string.record_environment
        )
    }

    private fun updateRecordingFolder() {
        val uri = config.callRecordingFolderUri
        binding.settingsRecordingFolder.text = if (uri.isBlank()) {
            getString(R.string.not_set)
        } else {
            DocumentFile.fromTreeUri(this, Uri.parse(uri))?.name ?: uri
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.calling_accounts -> launchAccountsConfiguration()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.settingsToolbar.menu.apply {
            findItem(R.id.calling_accounts).isVisible = canLaunchAccountsConfiguration()
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        binding.apply {
            settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
            settingsUseEnglish.isChecked = config.useEnglish
            settingsUseEnglishHolder.setOnClickListener {
                settingsUseEnglish.toggle()
                config.useEnglish = settingsUseEnglish.isChecked
                exitProcess(0)
            }
        }
    }

    private fun setupLanguage() {
        binding.apply {
            settingsLanguage.text = Locale.getDefault().displayLanguage
            settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
            settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        }
    }

    private fun setupManageBlockedNumbers() {
        binding.apply {
            settingsManageBlockedNumbersLabel.text = addLockedLabelIfNeeded(R.string.manage_blocked_numbers)
            settingsManageBlockedNumbersHolder.beVisibleIf(isNougatPlus())
            settingsManageBlockedNumbersHolder.setOnClickListener {
                if (isOrWasThankYouInstalled()) {
                    Intent(this@SettingsActivity, ManageBlockedNumbersActivity::class.java).apply {
                        startActivity(this)
                    }
                } else {
                    FeatureLockedDialog(this@SettingsActivity) { }
                }
            }
        }
    }

    private fun setupChangeDateTimeFormat() {
        binding.settingsChangeDateTimeFormatHolder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }
    }

    private fun setupFontSize() {
        binding.settingsFontSize.text = getFontSizeText()
        binding.settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                binding.settingsFontSize.text = getFontSizeText()
            }
        }
    }

    private fun setupDefaultTab() {
        binding.settingsDefaultTab.text = getDefaultTabText()
        binding.settingsDefaultTabHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TAB_CONTACTS, getString(R.string.contacts_tab)),
                RadioItem(TAB_CALL_HISTORY, getString(R.string.call_history_tab)),
                RadioItem(TAB_LAST_USED, getString(R.string.last_used_tab))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.defaultTab) {
                config.defaultTab = it as Int
                binding.settingsDefaultTab.text = getDefaultTabText()
            }
        }
    }

    private fun getDefaultTabText() = getString(
        when (baseConfig.defaultTab) {
            TAB_CONTACTS -> R.string.contacts_tab
            TAB_CALL_HISTORY -> R.string.call_history_tab
            else -> R.string.last_used_tab
        }
    )

    private fun setupOnContactClick() {
        binding.settingsOnContactClick.text = getOnContactClickText()
        binding.settingsOnContactClickHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ON_CLICK_CALL_CONTACT, getString(org.fossify.commons.R.string.call_contact)),
                RadioItem(ON_CLICK_VIEW_CONTACT, getString(org.fossify.commons.R.string.view_contact))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.onContactClick) {
                config.onContactClick = it as Int
                binding.settingsOnContactClick.text = getOnContactClickText()
            }
        }
    }

    private fun getOnContactClickText() = getString(
        when (config.onContactClick) {
            ON_CLICK_CALL_CONTACT -> org.fossify.commons.R.string.call_contact
            else -> org.fossify.commons.R.string.view_contact
        }
    )

    private fun setupDialPadOpen() {
        binding.apply {
            settingsOpenDialpadAtLaunch.isChecked = config.openDialPadAtLaunch
            settingsOpenDialpadAtLaunchHolder.setOnClickListener {
                settingsOpenDialpadAtLaunch.toggle()
                config.openDialPadAtLaunch = settingsOpenDialpadAtLaunch.isChecked
            }
        }
    }

    private fun setupGroupSubsequentCalls() {
        binding.apply {
            settingsGroupSubsequentCalls.isChecked = config.groupSubsequentCalls
            settingsGroupSubsequentCallsHolder.setOnClickListener {
                settingsGroupSubsequentCalls.toggle()
                config.groupSubsequentCalls = settingsGroupSubsequentCalls.isChecked
            }
        }
    }

    private fun setupStartNameWithSurname() {
        binding.apply {
            settingsStartNameWithSurname.isChecked = config.startNameWithSurname
            settingsStartNameWithSurnameHolder.setOnClickListener {
                settingsStartNameWithSurname.toggle()
                config.startNameWithSurname = settingsStartNameWithSurname.isChecked
            }
        }
    }

    private fun setupFormatPhoneNumbers() {
        binding.settingsFormatPhoneNumbers.isChecked = config.formatPhoneNumbers
        binding.settingsFormatPhoneNumbersHolder.setOnClickListener {
            binding.settingsFormatPhoneNumbers.toggle()
            config.formatPhoneNumbers = binding.settingsFormatPhoneNumbers.isChecked
        }
    }

    private fun setupDialpadVibrations() {
        binding.apply {
            settingsDialpadVibration.isChecked = config.dialpadVibration
            settingsDialpadVibrationHolder.setOnClickListener {
                settingsDialpadVibration.toggle()
                config.dialpadVibration = settingsDialpadVibration.isChecked
            }
        }
    }

    private fun setupDialpadNumbers() {
        binding.apply {
            settingsHideDialpadNumbers.isChecked = config.hideDialpadNumbers
            settingsHideDialpadNumbersHolder.setOnClickListener {
                settingsHideDialpadNumbers.toggle()
                config.hideDialpadNumbers = settingsHideDialpadNumbers.isChecked
            }
        }
    }

    private fun setupDialpadBeeps() {
        binding.apply {
            settingsDialpadBeeps.isChecked = config.dialpadBeeps
            settingsDialpadBeepsHolder.setOnClickListener {
                settingsDialpadBeeps.toggle()
                config.dialpadBeeps = settingsDialpadBeeps.isChecked
            }
        }
    }

    private fun setupShowCallConfirmation() {
        binding.apply {
            settingsShowCallConfirmation.isChecked = config.showCallConfirmation
            settingsShowCallConfirmationHolder.setOnClickListener {
                settingsShowCallConfirmation.toggle()
                config.showCallConfirmation = settingsShowCallConfirmation.isChecked
            }
        }
    }

    private fun setupDisableProximitySensor() {
        binding.apply {
            settingsDisableProximitySensor.isChecked = config.disableProximitySensor
            settingsDisableProximitySensorHolder.setOnClickListener {
                settingsDisableProximitySensor.toggle()
                config.disableProximitySensor = settingsDisableProximitySensor.isChecked
            }
        }
    }

    private fun setupDisableSwipeToAnswer() {
        binding.apply {
            settingsDisableSwipeToAnswer.isChecked = config.disableSwipeToAnswer
            settingsDisableSwipeToAnswerHolder.setOnClickListener {
                settingsDisableSwipeToAnswer.toggle()
                config.disableSwipeToAnswer = settingsDisableSwipeToAnswer.isChecked
            }
        }
    }

    private fun setupAlwaysShowFullscreen() {
        binding.apply {
            settingsAlwaysShowFullscreen.isChecked = config.alwaysShowFullscreen
            settingsAlwaysShowFullscreenHolder.setOnClickListener {
                settingsAlwaysShowFullscreen.toggle()
                config.alwaysShowFullscreen = settingsAlwaysShowFullscreen.isChecked
            }
        }
    }

}

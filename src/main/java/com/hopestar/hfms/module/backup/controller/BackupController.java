package com.hopestar.hfms.module.backup.controller;

import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.module.backup.dto.BackupScheduleUpdateDTO;
import com.hopestar.hfms.module.backup.service.BackupScheduleService;
import com.hopestar.hfms.module.backup.service.BackupService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for the Backup & Restore page (Module 8), including the
 * Backup Schedule Control section. Thin, per the module architecture --
 * {@link BackupService}/{@link BackupScheduleService} carry the real logic
 * and the {@code SETTINGS_MANAGE} authorization check (defense in depth:
 * the URL itself only requires an authenticated user, per {@code
 * SecurityConfig}'s comment referencing this exact route).
 */
@Controller
@RequestMapping("/settings/backup")
public class BackupController {

    private final BackupService backupService;
    private final BackupScheduleService backupScheduleService;

    public BackupController(BackupService backupService, BackupScheduleService backupScheduleService) {
        this.backupService = backupService;
        this.backupScheduleService = backupScheduleService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("backups", backupService.listBackups());
        populateScheduleForm(model);
        return "settings/backup";
    }

    @PostMapping
    public String backupNow(RedirectAttributes redirectAttributes) {
        try {
            var created = backupService.createBackup(BackupType.MANUAL, SecurityUtil.currentUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Backup '" + created.getFileName() + "' completed successfully.");
        } catch (BusinessValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/backup";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            backupService.restoreBackup(id, SecurityUtil.currentUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Database restored successfully from backup id " + id + ".");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/backup";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            backupService.deleteBackup(id);
            redirectAttributes.addFlashAttribute("successMessage", "Backup deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/backup";
    }

    @PostMapping("/schedule")
    public String updateSchedule(@Valid @ModelAttribute("scheduleDTO") BackupScheduleUpdateDTO scheduleDTO,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please choose a valid time.");
            return "redirect:/settings/backup";
        }
        try {
            var updated = backupScheduleService.updateSchedule(scheduleDTO);
            redirectAttributes.addFlashAttribute("successMessage", updated.isEnabled()
                    ? "Automatic backups are now ON, running nightly at " + updated.getTime() + "."
                    : "Automatic backups are now OFF.");
        } catch (BusinessValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/backup";
    }

    private void populateScheduleForm(Model model) {
        var schedule = backupScheduleService.getSchedule();
        BackupScheduleUpdateDTO scheduleDTO = new BackupScheduleUpdateDTO();
        scheduleDTO.setEnabled(schedule.isEnabled());
        scheduleDTO.setTime(schedule.getTime());

        model.addAttribute("schedule", schedule);
        model.addAttribute("scheduleDTO", scheduleDTO);
    }
}

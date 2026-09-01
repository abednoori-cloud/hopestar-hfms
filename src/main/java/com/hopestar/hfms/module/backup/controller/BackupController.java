package com.hopestar.hfms.module.backup.controller;

import com.hopestar.hfms.common.enums.BackupType;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.module.backup.service.BackupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for the Backup & Restore page (Module 8). Thin, per the
 * module architecture -- {@link BackupService} carries the real logic and
 * the {@code SETTINGS_MANAGE} authorization check (defense in depth: the
 * URL itself only requires an authenticated user, per {@code
 * SecurityConfig}'s comment referencing this exact route).
 */
@Controller
@RequestMapping("/settings/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("backups", backupService.listBackups());
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
}

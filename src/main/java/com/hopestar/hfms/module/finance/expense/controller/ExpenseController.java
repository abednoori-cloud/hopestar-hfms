package com.hopestar.hfms.module.finance.expense.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.LogoService;
import com.hopestar.hfms.common.service.PdfGenerationService;
import com.hopestar.hfms.module.auth.service.BranchService;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseCreateDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseResponseDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseSearchDTO;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseUpdateDTO;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import com.hopestar.hfms.module.finance.expense.service.ExpenseCategoryService;
import com.hopestar.hfms.module.finance.expense.service.ExpenseService;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import com.hopestar.hfms.module.finance.ledger.service.LedgerService;
import com.hopestar.hfms.module.finance.ledger.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * MVC controller for the Expenses module: list/search/sort/paginate,
 * view, create draft, edit draft, post, void. Thin per the module
 * architecture -- mirrors {@code EmployeeAdvanceController}'s shape,
 * minus the repayment routes an expense has no concept of. All business
 * logic lives in {@link ExpenseService}.
 */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseCategoryService expenseCategoryService;
    private final PaymentMethodService paymentMethodService;
    private final LedgerService ledgerService;
    private final BranchService branchService;
    private final PdfGenerationService pdfGenerationService;
    private final LogoService logoService;
    private final TemplateEngine pdfTemplateEngine;

    public ExpenseController(ExpenseService expenseService,
                              ExpenseCategoryService expenseCategoryService,
                              PaymentMethodService paymentMethodService,
                              LedgerService ledgerService,
                              BranchService branchService,
                              PdfGenerationService pdfGenerationService,
                              LogoService logoService,
                              @Qualifier("pdfTemplateEngine") TemplateEngine pdfTemplateEngine) {
        this.expenseService = expenseService;
        this.expenseCategoryService = expenseCategoryService;
        this.paymentMethodService = paymentMethodService;
        this.ledgerService = ledgerService;
        this.branchService = branchService;
        this.pdfGenerationService = pdfGenerationService;
        this.logoService = logoService;
        this.pdfTemplateEngine = pdfTemplateEngine;
    }

    // ---------------------------------------------------------------
    // List / Search / Sort / Paginate
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") ExpenseSearchDTO searchDTO, Model model) {
        PageResponse<ExpenseResponseDTO> expensesPage = expenseService.search(searchDTO);
        model.addAttribute("expensesPage", expensesPage);
        model.addAttribute("searchDTO", searchDTO);
        model.addAttribute("categories", expenseCategoryService.listActive());
        return "expenses/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("createDTO", new ExpenseCreateDTO());
        populateFormLookups(model);
        model.addAttribute("isEdit", false);
        return "expenses/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createDTO") ExpenseCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model);
            model.addAttribute("isEdit", false);
            return "expenses/form";
        }
        try {
            ExpenseResponseDTO created = expenseService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Expense " + created.getExpenseNumber() + " was created as a draft.");
            return "redirect:/expenses/" + created.getId();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model);
            model.addAttribute("isEdit", false);
            return "expenses/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("expense", expenseService.getById(id));
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        return "expenses/view";
    }

    // ---------------------------------------------------------------
    // Voucher PDF
    // ---------------------------------------------------------------

    /**
     * Renders and streams a downloadable PDF voucher for this expense.
     * Purely a printable representation of the already-existing {@code
     * Expense} record -- generated on demand, nothing persisted to disk
     * (see the Receipt/Voucher PDF design notes).
     */
    @GetMapping("/{id}/voucher/pdf")
    public ResponseEntity<byte[]> voucherPdf(@PathVariable Long id) {
        ExpenseResponseDTO expense = expenseService.getById(id);

        String paymentMethodName = null;
        boolean voided = false;
        if (expense.getLedgerTransactionId() != null) {
            TransactionResponseDTO transaction = ledgerService.findTransaction(expense.getLedgerTransactionId());
            paymentMethodName = transaction.getPaymentMethod().getName();
            voided = transaction.getStatus() == TransactionStatus.VOIDED;
        }

        Context context = new Context();
        context.setVariable("expense", expense);
        context.setVariable("office", branchService.getHeadquarters());
        context.setVariable("logoDataUri", logoService.getLogoDataUri());
        context.setVariable("paymentMethodName", paymentMethodName);
        context.setVariable("voided", voided);

        String html = pdfTemplateEngine.process("expenses/voucher-pdf", context);
        byte[] pdfBytes = pdfGenerationService.renderToPdf(html);

        String filename = "Voucher-" + expense.getExpenseNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ---------------------------------------------------------------
    // Edit Draft
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        ExpenseResponseDTO existing = expenseService.getById(id);
        if (existing.getStatus() != ExpenseStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Expense " + existing.getExpenseNumber() + " can no longer be edited because it is "
                            + existing.getStatus() + ", not DRAFT.");
            return "redirect:/expenses/" + id;
        }

        ExpenseUpdateDTO updateDTO = new ExpenseUpdateDTO();
        updateDTO.setCategoryId(existing.getCategoryId());
        updateDTO.setDescription(existing.getDescription());
        updateDTO.setAmount(existing.getAmount());
        updateDTO.setCurrency(existing.getCurrency());
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setExpenseDate(existing.getExpenseDate());
        updateDTO.setRemarks(existing.getRemarks());

        model.addAttribute("expenseId", id);
        model.addAttribute("expense", existing);
        model.addAttribute("updateDTO", updateDTO);
        populateFormLookups(model);
        model.addAttribute("isEdit", true);
        return "expenses/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("updateDTO") ExpenseUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("expenseId", id);
            model.addAttribute("expense", expenseService.getById(id));
            populateFormLookups(model);
            model.addAttribute("isEdit", true);
            return "expenses/form";
        }
        try {
            ExpenseResponseDTO updated = expenseService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Expense " + updated.getExpenseNumber() + " was updated.");
            return "redirect:/expenses/" + id;
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("expenseId", id);
            model.addAttribute("expense", expenseService.getById(id));
            populateFormLookups(model);
            model.addAttribute("isEdit", true);
            return "expenses/form";
        }
    }

    // ---------------------------------------------------------------
    // Post / Void
    // ---------------------------------------------------------------

    @PostMapping("/{id}/post")
    public String post(@PathVariable Long id,
                        @RequestParam Long paymentMethodId,
                        RedirectAttributes redirectAttributes) {
        try {
            ExpenseResponseDTO posted = expenseService.post(id, paymentMethodId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Expense " + posted.getExpenseNumber() + " was posted.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidExpense(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            ExpenseResponseDTO voided = expenseService.voidExpense(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Expense " + voided.getExpenseNumber() + " was voided.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void populateFormLookups(Model model) {
        model.addAttribute("categories", expenseCategoryService.listActive());
    }
}

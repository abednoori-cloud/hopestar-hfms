package com.hopestar.hfms.module.reports.controller;

import com.hopestar.hfms.common.service.LogoService;
import com.hopestar.hfms.common.service.PdfGenerationService;
import com.hopestar.hfms.module.auth.service.BranchService;
import com.hopestar.hfms.module.reports.dto.CashFlowReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.EmployeeReportResponseDTO;
import com.hopestar.hfms.module.reports.dto.StudentReportResponseDTO;
import com.hopestar.hfms.module.reports.service.ReportService;
import com.hopestar.hfms.module.student.service.ProgramService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * MVC controller for the Reports module: three reports, each with an
 * on-screen view (date-range filter form + results) and a PDF download.
 * Thin per the module architecture -- all aggregation lives in {@link
 * ReportService}; this class only resolves the date-range default,
 * assembles the view model, and (for the PDF routes) renders the same
 * data through the PDF template engine, mirroring exactly how {@code
 * StudentPaymentController}/{@code ExpenseController} generate the
 * Receipt/Voucher PDFs.
 */
@Controller
public class ReportController {

    private final ReportService reportService;
    private final ProgramService programService;
    private final BranchService branchService;
    private final PdfGenerationService pdfGenerationService;
    private final LogoService logoService;
    private final TemplateEngine pdfTemplateEngine;

    public ReportController(ReportService reportService,
                             ProgramService programService,
                             BranchService branchService,
                             PdfGenerationService pdfGenerationService,
                             LogoService logoService,
                             @Qualifier("pdfTemplateEngine") TemplateEngine pdfTemplateEngine) {
        this.reportService = reportService;
        this.programService = programService;
        this.branchService = branchService;
        this.pdfGenerationService = pdfGenerationService;
        this.logoService = logoService;
        this.pdfTemplateEngine = pdfTemplateEngine;
    }

    // ---------------------------------------------------------------
    // Index
    // ---------------------------------------------------------------

    @GetMapping("/reports")
    public String index() {
        return "reports/index";
    }

    // ---------------------------------------------------------------
    // Report 1: Money In/Out
    // ---------------------------------------------------------------

    @GetMapping("/reports/cashflow")
    public String cashFlow(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            Model model) {
        LocalDate[] range = resolveRange(from, to);
        model.addAttribute("report", reportService.getCashFlowReport(range[0], range[1]));
        return "reports/cashflow";
    }

    @GetMapping("/reports/cashflow/pdf")
    public ResponseEntity<byte[]> cashFlowPdf(@RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        CashFlowReportResponseDTO report = reportService.getCashFlowReport(range[0], range[1]);

        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("office", branchService.getHeadquarters());
        context.setVariable("logoDataUri", logoService.getLogoDataUri());

        String html = pdfTemplateEngine.process("reports/cashflow-pdf", context);
        byte[] pdfBytes = pdfGenerationService.renderToPdf(html);
        return downloadResponse(pdfBytes, "MoneyInOutReport-" + range[0] + "-to-" + range[1] + ".pdf");
    }

    // ---------------------------------------------------------------
    // Report 2: Student Report
    // ---------------------------------------------------------------

    @GetMapping("/reports/students")
    public String students(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            @RequestParam(required = false) Long programId,
                            Model model) {
        LocalDate[] range = resolveRange(from, to);
        model.addAttribute("report", reportService.getStudentReport(range[0], range[1], programId));
        model.addAttribute("programs", programService.listActive());
        model.addAttribute("selectedProgramId", programId);
        return "reports/students";
    }

    @GetMapping("/reports/students/pdf")
    public ResponseEntity<byte[]> studentsPdf(@RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to,
                                               @RequestParam(required = false) Long programId) {
        LocalDate[] range = resolveRange(from, to);
        StudentReportResponseDTO report = reportService.getStudentReport(range[0], range[1], programId);

        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("office", branchService.getHeadquarters());
        context.setVariable("logoDataUri", logoService.getLogoDataUri());

        String html = pdfTemplateEngine.process("reports/students-pdf", context);
        byte[] pdfBytes = pdfGenerationService.renderToPdf(html);
        return downloadResponse(pdfBytes, "StudentReport-" + range[0] + "-to-" + range[1] + ".pdf");
    }

    // ---------------------------------------------------------------
    // Report 3: Employee Report
    // ---------------------------------------------------------------

    @GetMapping("/reports/employees")
    public String employees(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             Model model) {
        LocalDate[] range = resolveRange(from, to);
        model.addAttribute("report", reportService.getEmployeeReport(range[0], range[1]));
        return "reports/employees";
    }

    @GetMapping("/reports/employees/pdf")
    public ResponseEntity<byte[]> employeesPdf(@RequestParam(required = false) LocalDate from,
                                                @RequestParam(required = false) LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        EmployeeReportResponseDTO report = reportService.getEmployeeReport(range[0], range[1]);

        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("office", branchService.getHeadquarters());
        context.setVariable("logoDataUri", logoService.getLogoDataUri());

        String html = pdfTemplateEngine.process("reports/employees-pdf", context);
        byte[] pdfBytes = pdfGenerationService.renderToPdf(html);
        return downloadResponse(pdfBytes, "EmployeeReport-" + range[0] + "-to-" + range[1] + ".pdf");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    /** Defaults to the current calendar month when either bound is omitted, mirroring DashboardServiceImpl's period math. */
    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return new LocalDate[] {from, to};
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate defaultFrom = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate defaultTo = today.with(TemporalAdjusters.lastDayOfMonth());
        return new LocalDate[] {from != null ? from : defaultFrom, to != null ? to : defaultTo};
    }

    private ResponseEntity<byte[]> downloadResponse(byte[] pdfBytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

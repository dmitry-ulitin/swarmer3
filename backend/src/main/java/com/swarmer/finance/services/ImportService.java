package com.swarmer.finance.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.CategoryDto;
import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.RuleDto;
import com.swarmer.finance.models.BankType;
import com.swarmer.finance.models.ConditionType;
import com.swarmer.finance.models.Rule;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.repositories.RuleRepository;

import jakarta.transaction.Transactional;

@Service
public class ImportService {
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final RuleRepository ruleRepository;

    public ImportService(TransactionService transactionService, CategoryService categoryService,
            RuleRepository ruleRepository) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.ruleRepository = ruleRepository;

    }

    @Transactional
    public List<RuleDto> getRules(Long userId) {
        return ruleRepository.findAllByOwnerId(userId).stream()
                .map(RuleDto::fromEntity)
                .toList();
    }

    @Transactional
    public RuleDto createRule(RuleDto ruleDto, Long userId) {
        Rule rule = new Rule();
        rule.setOwnerId(userId);
        rule.setConditionType(ruleDto.conditionType());
        rule.setConditionValue(ruleDto.conditionValue());
        rule.setCategory(categoryService.getCategory(ruleDto.category(), userId));
        rule = ruleRepository.save(rule);
        return RuleDto.fromEntity(rule);
    }

    @Transactional
    public RuleDto updateRule(Long id, RuleDto ruleDto, Long userId) {
        Rule rule = ruleRepository.findById(id).orElseThrow();
        rule.setConditionType(ruleDto.conditionType());
        rule.setConditionValue(ruleDto.conditionValue());
        rule.setCategory(categoryService.getCategory(ruleDto.category(), userId));
        rule.setUpdated(LocalDateTime.now());
        rule = ruleRepository.save(rule);
        return RuleDto.fromEntity(rule);
    }

    @Transactional
    public void deleteRule(Long id, Long userId) {
        Rule rule = ruleRepository.findById(id).orElseThrow();
        if (!rule.getOwnerId().equals(userId)) {
            throw new SecurityException("You are not authorized to delete this rule");
        }
        ruleRepository.deleteById(id);
    }

    @Transactional
    public List<ImportDto> importFile(InputStream is, BankType bankId, Long accountId, Long userId)
            throws UnsupportedEncodingException, IOException, ParseException {
        List<ImportDto> records = switch (bankId) {
            case LHV -> importLhv(is);
            case TINKOFF -> importTinkoff(is);
            case SBER -> importSber(is);
            case ALFABANK -> importAlfabank(is);
            case UNICREDIT -> importUnicredit(is);
            case CAIXA -> importCaixa(is);
            default -> throw new IllegalArgumentException("Unknown bank type: " + bankId);
        };
        return importRecords(records, accountId, userId);
    }

    public List<ImportDto> importRecords(List<ImportDto> records, Long accountId, Long userId) {
        if (records.isEmpty()) {
            return records;
        }
        var minOpdate = records.stream().map(r -> r.getOpdate()).min((a, b) -> a.compareTo(b)).orElse(null);
        var transactions = transactionService.queryTransactions(userId, List.of(accountId), null, null, minOpdate, null,
                0, 0);
        var rules = getRules(userId);
        var rmap = rules.stream()
                .collect(Collectors.groupingBy(rule -> Pair.of(rule.conditionType(), rule.category().type())));
        records.forEach(r -> {
            RuleDto rule = null;
            if (r.getDetails() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.DETAILS_EQUALS, r.getType()), List.of());
                rule = list.stream().filter(rl -> rl.conditionValue().equalsIgnoreCase(r.getDetails())).findFirst()
                        .orElse(null);
            }
            if (rule == null && r.getParty() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.PARTY_EQUALS, r.getType()), List.of());
                rule = list.stream().filter(rl -> rl.conditionValue().equalsIgnoreCase(r.getParty())).findFirst()
                        .orElse(null);
            }
            if (rule == null && r.getCatname() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.CATNAME_EQUALS, r.getType()), List.of());
                rule = list.stream().filter(rl -> rl.conditionValue().equalsIgnoreCase(r.getCatname())).findFirst()
                        .orElse(null);
            }
            if (rule == null && r.getDetails() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.DETAILS_CONTAINS, r.getType()), List.of());
                rule = list.stream()
                        .filter(rl -> r.getDetails().toLowerCase().contains(rl.conditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule == null && r.getParty() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.PARTY_CONTAINS, r.getType()), List.of());
                rule = list.stream()
                        .filter(rl -> r.getParty().toLowerCase().contains(rl.conditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule == null && r.getCatname() != null) {
                var list = rmap.getOrDefault(Pair.of(ConditionType.CATNAME_CONTAINS, r.getType()), List.of());
                rule = list.stream()
                        .filter(rl -> r.getCatname().toLowerCase().contains(rl.conditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule != null) {
                r.setCategory(rule.category());
                r.setRule(rule);
            }
            var transaction = transactions.stream()
                    .filter(t -> t.getOpdate().toLocalDate().equals(r.getOpdate().toLocalDate())
                            && (t.getAccount() != null && t.getAccount().getId().equals(accountId)
                                    && t.getDebit().setScale(t.getAccount().getScale(), RoundingMode.HALF_DOWN).equals(
                                            r.getDebit().setScale(t.getAccount().getScale(), RoundingMode.HALF_DOWN))
                                    || t.getRecipient() != null && t.getRecipient().getId().equals(accountId)
                                            && t.getCredit()
                                                    .setScale(t.getRecipient().getScale(), RoundingMode.HALF_DOWN)
                                                    .equals(r.getCredit().setScale(t.getRecipient().getScale(),
                                                            RoundingMode.HALF_DOWN))))
                    .findFirst().orElse(null);
            if (transaction != null) {
                r.setId(transaction.getId());
                r.setSelected(false);
                r.setCategory(CategoryDto.fromEntity(transaction.getCategory()));
                transactions.remove(transaction);
            } else {
                r.setSelected(true);
            }
        });
        return records;
    }

    private List<ImportDto> importLhv(InputStream is) throws UnsupportedEncodingException, IOException {
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true)
                .setTrim(true).get();
        try (var fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                var csvParser = CSVParser.parse(fileReader, format)) {
            return csvParser.getRecords().stream().map(r -> {
                var type = "D".equals(r.get(7)) ? TransactionType.EXPENSE : TransactionType.INCOME;
                var opdate = LocalDate.parse(r.get(2), DateTimeFormatter.ISO_DATE).atStartOfDay();
                var debit = new BigDecimal(r.get(8)).abs();
                var credit = debit;
                var currency = r.get(13);
                var party = r.get(4);
                var details = r.get(11);
                return new ImportDto(null, opdate, type, debit, credit, null, null, currency, party, details, null,
                        true);
            }).toList();
        }
    }

    private List<ImportDto> importTinkoff(InputStream is)
            throws UnsupportedEncodingException, IOException, ParseException {
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true)
                .setTrim(true).setDelimiter(';').get();
        try (var fileReader = new BufferedReader(new InputStreamReader(is, "cp1251"));
                var csvParser = CSVParser.parse(fileReader, format)) {
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.FRANCE));
            decimalFormat.setParseBigDecimal(true);
            List<ImportDto> records = new ArrayList<>();
            for (var r : csvParser.getRecords()) {
                var amount = (BigDecimal) decimalFormat.parse(r.get(4));
                var type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
                var opdate = LocalDateTime.parse(r.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                var currency = r.get(5);
                var catname = r.get(9);
                var details = r.get(11);
                records.add(new ImportDto(null, opdate, type, amount.abs(), amount.abs(), null, null, currency, null,
                        details, catname, true));
            }
            return records;
        }
    }

    private List<ImportDto> importSber(InputStream is) throws ParseException, IOException {
        try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println(text);
            String[] lines = text.split("\\R");
            boolean table = false;
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.FRANCE));
            decimalFormat.setParseBigDecimal(true);
            List<ImportDto> records = new ArrayList<>();
            for (int i = 0; i < (lines.length - 1); i++) {
                if (table) {
                    if (lines[i].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                        var opdate = LocalDateTime.parse(lines[i].substring(0, 16),
                                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                        lines[i] = lines[i].substring(17);
                        var parts = lines[i].split("\\s");
                        var amount = (BigDecimal) decimalFormat
                                .parse(parts[parts.length - 2].replaceAll("\\u00a0|\\+|-", ""));
                        var type = parts[parts.length - 2].startsWith("+") ? TransactionType.INCOME
                                : TransactionType.EXPENSE;
                        lines[i] = lines[i].substring(parts[0].length() + 1, lines[i].length()
                                - parts[parts.length - 2].length() - parts[parts.length - 1].length() - 2);
                        var catname = lines[i++];
                        lines[i] = lines[i].substring(11);
                        String details = lines[i];
                        if (!lines[i + 1].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                            if (lines[i + 2].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                                details += " " + lines[++i];
                            } else if (lines[i + 3].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                                details += " " + lines[++i] + " " + lines[++i];
                            }
                        }
                        records.add(
                                new ImportDto(null, opdate, type, amount.abs(), amount.abs(), null, null,
                                        "RUB", null, details.trim(), catname, true));
                    } else {
                        table = false;
                    }
                } else if (lines[i + 1].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                    table = true;
                }
            }
            return records;
        }
    }

    private List<ImportDto> importAlfabank(InputStream is) throws EncryptedDocumentException, IOException {
        try (var workbook = WorkbookFactory.create(is)) {
            var sheet = workbook.getSheetAt(0);
            List<ImportDto> records = new ArrayList<>();
            for (Row row : sheet) {
                var date = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
                if (!date.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    continue;
                }
                var opdate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                var catname = row.getCell(10).getStringCellValue();
                var details = row.getCell(6).getStringCellValue();
                var currency = row.getCell(8).getStringCellValue();
                var amount = new BigDecimal(row.getCell(7).getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
                var type = row.getCell(12).getStringCellValue().equals("Пополнение") ? TransactionType.INCOME
                        : TransactionType.EXPENSE;
                records.add(new ImportDto(null, opdate, type, amount.abs(), amount.abs(), null, null,
                        currency.equals("RUR") ? "RUB" : currency, null, details, catname, true));
            }
            return records;
        }
    }

    private List<ImportDto> importUnicredit(InputStream is) throws EncryptedDocumentException, IOException {
        try (var workbook = WorkbookFactory.create(is)) {
            var sheet = workbook.getSheetAt(0);
            List<ImportDto> records = new ArrayList<>();
            for (Row row : sheet) {
                var date = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
                if (!date.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    continue;
                }
                var opdate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                var details = row.getCell(14).getStringCellValue();
                var currency = row.getCell(2).getStringCellValue();
                var amount = new BigDecimal(row.getCell(1).getStringCellValue().replace(",", "")).setScale(2,
                        RoundingMode.HALF_UP);
                var type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
                records.add(new ImportDto(null, opdate, type, amount.abs(), amount.abs(), null, null, currency, null,
                        details, null, true));
            }
            return records;
        }
    }

    private List<ImportDto> importCaixa(InputStream is) throws EncryptedDocumentException, IOException {
        try (var workbook = WorkbookFactory.create(is)) {
            var sheet = workbook.getSheetAt(0);
            var base = LocalDate.of(1899, 12, 30);
            List<ImportDto> records = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() != CellType.NUMERIC) {
                    continue;
                }
                var opdate = base.plusDays((long) row.getCell(0).getNumericCellValue()).atStartOfDay();
                var party = row.getCell(2).getStringCellValue();
                var details = row.getCell(3).getStringCellValue();
                var amount = new BigDecimal(row.getCell(4).getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
                var type = amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
                records.add(new ImportDto(null, opdate, type, amount.abs(), amount.abs(), null, null, "EUR", party,
                        details, null, true));
            }
            return records;
        }
    }
}

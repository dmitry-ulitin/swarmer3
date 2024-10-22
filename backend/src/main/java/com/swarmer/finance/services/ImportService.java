package com.swarmer.finance.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.ImportDto;
import com.swarmer.finance.dto.RuleDto;
import com.swarmer.finance.models.BankType;
import com.swarmer.finance.models.ConditionType;
import com.swarmer.finance.models.Rule;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.repositories.RuleRepository;

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

    public List<RuleDto> getRules(Long userId) {
        return ruleRepository.findAllByOwnerId(userId).stream().map(r -> RuleDto.from(r)).toList();
    }

    public RuleDto getRuleById(Long ruleId, Long userId) {
        return ruleRepository.findById(ruleId).map(r -> RuleDto.from(r)).orElse(null);
    }

    public RuleDto addRule(RuleDto rule, Long userId) {
        var entity = new Rule();
        entity.setOwnerId(userId);
        entity.setConditionType(rule.conditionType());
        entity.setConditionValue(rule.conditionValue());
        if (rule.category() != null) {
            var category = categoryService.getCategory(rule.category(), userId);
            entity.setCategory(category);
        }
        entity.setCreated(LocalDateTime.now());
        entity.setUpdated(LocalDateTime.now());
        return RuleDto.from(ruleRepository.save(entity));
    }

    public RuleDto updateRule(RuleDto rule, Long userId) {
        var entity = ruleRepository.findById(rule.id()).orElseThrow();
        entity.setConditionType(rule.conditionType());
        entity.setConditionValue(rule.conditionValue());
        if (rule.category() != null) {
            var category = categoryService.getCategory(rule.category(), userId);
            entity.setCategory(category);
        }
        entity.setUpdated(LocalDateTime.now());
        return RuleDto.from(ruleRepository.save(entity));
    }

    public void deleteRule(Long ruleId, Long userId) {
        var entity = ruleRepository.findById(ruleId).orElseThrow();
        if (!entity.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException();
        }
        ruleRepository.deleteById(ruleId);
    }

    public List<ImportDto> importFile(InputStream is, BankType bankId, Long accountId, Long userId)
            throws IOException, ParseException, NumberFormatException {
        List<ImportDto> records = new ArrayList<>();
        if (bankId == BankType.SBER) {
            try (PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                System.out.println(text);
                String[] lines = text.split("\\R");
                boolean table = false;
                NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
                for (int i = 0; i < (lines.length - 1); i++) {
                    if (table) {
                        if (lines[i].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                            var opdate = LocalDateTime.parse(lines[i].substring(0, 16),
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                            lines[i] = lines[i].substring(17);
                            var parts = lines[i].split("\\s");
                            var amount = nf.parse(parts[parts.length - 2].replaceAll("\\u00a0|\\+|-", ""))
                                    .doubleValue();
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
                                    new ImportDto(null, opdate, type, Math.abs(amount), Math.abs(amount), null, null,
                                            "RUB", null, details.trim(), catname, true));
                        } else {
                            table = false;
                        }
                    } else if (lines[i + 1].matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2} \\d+ .*")) {
                        table = true;
                    }
                }
            }
        } else if (bankId == BankType.ALFABANK) {
            try (var wb = new org.dhatim.fastexcel.reader.ReadableWorkbook(is)) {
                var sheet = wb.getFirstSheet();
                var rows = sheet.openStream().skip(1).toList();
                for (var row : rows) {
                    if (!row.getCell(0).asString().matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                        continue;
                    }
                    var opdate = LocalDate.parse(row.getCell(0).asString(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            .atStartOfDay();
                    var type = row.getCell(12).asString().equals("Пополнение") ? TransactionType.INCOME
                            : TransactionType.EXPENSE;
                    var debit = row.getCell(7).asNumber().doubleValue();
                    var credit = row.getCell(7).asNumber().doubleValue();
                    var currency = row.getCell(8).asString();
                    var catname = row.getCell(10).asString();
                    var details = row.getCell(6).asString();
                    records.add(new ImportDto(null, opdate, type, debit, credit, null, null,
                            currency.equals("RUR") ? "RUB" : currency, null, details, catname, true));
                }
            }
        } else if (bankId == BankType.UNICREDIT) {
            try (var wb = new org.dhatim.fastexcel.reader.ReadableWorkbook(is)) {
                var sheet = wb.getFirstSheet();
                var rows = sheet.openStream().skip(4).toList();
                for (var row : rows) {
                    String date = row.getCell(4).asString().trim();;
                    if (!date.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                        continue;
                    }
                    var opdate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                    var debit = Double.parseDouble(row.getCell(1).asString().trim().replace(",", ""));
                    var credit = debit;
                    var type = debit > 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
                    var currency = row.getCell(2).asString().trim();
                    var details = row.getCell(14).asString().trim();
                    records.add(new ImportDto(null, opdate, type, Math.abs(debit), Math.abs(credit), null, null,
                            currency.equals("RUR") ? "RUB" : currency, null, details, null, true));
                }
            }
        } else {
            var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true)
                    .setTrim(true).build();
            if (bankId == BankType.TINKOFF) {
                format = format.builder().setDelimiter(';').build();
            }
            try (var fileReader = new BufferedReader(
                    new InputStreamReader(is, bankId == BankType.TINKOFF ? "cp1251" : "UTF-8"));
                    var csvParser = new CSVParser(fileReader, format)) {
                records = csvParser.getRecords().stream().map(r -> csv2trx(bankId, r)).toList();
            }
        }
        var minOpdate = records.stream().map(r -> r.getOpdate()).min((a, b) -> a.compareTo(b)).orElseThrow();
        var trx = transactionService.queryTransactions(userId, List.of(accountId), null, null, minOpdate, null, 0, 0);
        var rules = ruleRepository.findAllByOwnerId(userId);

        return records.stream().map(r -> {
            var rule = rules
                    .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                            && rl.getConditionType() == ConditionType.PARTY_EQUALS
                            && rl.getConditionValue().equals(r.getParty()))
                    .findFirst().orElse(null);
            if (rule == null) {
                rule = rules
                        .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                                && rl.getConditionType() == ConditionType.DETAILS_EQUALS
                                && rl.getConditionValue().equals(r.getDetails()))
                        .findFirst().orElse(null);
            }
            if (rule == null) {
                rule = rules
                        .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                                && rl.getConditionType() == ConditionType.CATNAME_EQUALS
                                && rl.getConditionValue().equals(r.getCatname()))
                        .findFirst().orElse(null);
            }
            if (rule == null) {
                rule = rules
                        .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                                && rl.getConditionType() == ConditionType.PARTY_CONTAINS && r.getParty() != null
                                && r.getParty().toLowerCase().contains(rl.getConditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule == null) {
                rule = rules
                        .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                                && rl.getConditionType() == ConditionType.DETAILS_CONTAINS && r.getDetails() != null
                                && r.getDetails().toLowerCase().contains(rl.getConditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule == null) {
                rule = rules
                        .stream().filter(rl -> rl.getCategory().getType().equals(r.getType())
                                && rl.getConditionType() == ConditionType.CATNAME_CONTAINS && r.getCatname() != null
                                && r.getCatname().toLowerCase().contains(rl.getConditionValue().toLowerCase()))
                        .findFirst().orElse(null);
            }
            if (rule != null) {
                r.setRule(RuleDto.from(rule));
                r.setCategory(rule.getCategory());
            }
            var stored = trx.stream()
                    .filter(t -> t.getOpdate().toLocalDate().equals(r.getOpdate().toLocalDate())
                            && (t.getAccount() != null
                                    && t.getAccount().getId().equals(accountId) && t.getDebit() == r.getDebit()
                                    || t.getRecipient() != null && t.getRecipient().getId().equals(accountId)
                                            && t.getCredit() == r.getCredit()))
                    .findFirst().orElse(null);
            if (stored != null) {
                r.setId(stored.getId());
                r.setSelected(false);
                r.setCategory(stored.getCategory());
                trx.remove(stored);
            }
            return r;
        }).toList();
    }

    private ImportDto csv2trx(BankType bankId, CSVRecord r) {
        if (bankId == BankType.LHV) {
            var type = "D".equals(r.get(7)) ? TransactionType.EXPENSE : TransactionType.INCOME;
            var opdate = LocalDate.parse(r.get(2), DateTimeFormatter.ISO_DATE).atStartOfDay();
            var debit = Math.abs(Double.parseDouble(r.get(8)));
            var credit = debit;
            var currency = r.get(13);
            var party = r.get(4);
            var details = r.get(11);
            return new ImportDto(null, opdate, type, debit, credit, null, null, currency, party, details, null, true);
        } else if (bankId == BankType.TINKOFF) {
            NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
            try {
                var debit = nf.parse(r.get(4)).doubleValue();
                var credit = debit;
                var type = debit < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
                var opdate = LocalDateTime.parse(r.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                var currency = r.get(5);
                var catname = r.get(9);
                var details = r.get(11);
                return new ImportDto(null, opdate, type, Math.abs(debit), Math.abs(credit), null, null, currency, null,
                        details, catname, true);
            } catch (ParseException e) {
                throw (new NumberFormatException());
            }
        }
        throw (new IllegalArgumentException());
    }
}

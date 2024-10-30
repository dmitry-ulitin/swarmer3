package com.swarmer.finance.services;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.swarmer.finance.models.Category;
import com.swarmer.finance.repositories.CategoryRepository;
import com.swarmer.finance.repositories.RuleRepository;
import com.swarmer.finance.repositories.TransactionRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final AclService aclService;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository,
            RuleRepository ruleRepository, AclService aclService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
        this.aclService = aclService;
    }

    public List<Category> getCategories(Long userId) {
        var coowners = aclService.findUsers(userId);
        var comparator = Comparator.comparing(Category::getType)
                .thenComparingInt(c -> c.getParentId() == null ? -1 : 1)
                .thenComparing((c1, c2) -> c1.getFullName().compareToIgnoreCase(c2.getFullName()))
                .thenComparingInt(c -> userId.equals(c.getOwnerId()) ? -1 : 1);
        var categories = categoryRepository
                .findByOwnerIdIsNullOrOwnerIdIn(coowners.stream().distinct().toList())
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        Category prev = null;
        List<Category> result = new ArrayList<>();
        for (var c : categories) {
            if (prev != null && c.getFullName().toLowerCase().equals(prev.getFullName().toLowerCase())) {
                continue;
            }
            result.add(c);
            prev = c;
        }
        return result;
    }

    public List<Long> getCategoriesFilter(Long userId, Long categoryId) {
        List<Long> result = new ArrayList<>();
        if (categoryId == null) {
            return result;
        }
        var coowners = aclService.findUsers(userId);
        var comparator = Comparator.comparing(Category::getType)
                .thenComparingInt(c -> c.getParentId() == null ? 0 : 1)
                .thenComparing((c1, c2) -> c1.getFullName().compareToIgnoreCase(c2.getFullName()));
        var categories = categoryRepository
                .findByOwnerIdIsNullOrOwnerIdIn(coowners.stream().distinct().toList())
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        int index = 0;
        while (index < categories.size() && !categories.get(index).getId().equals(categoryId))
            index++;
        if (index > categories.size()) {
            result.add(categoryId);
            return result;
        }
        var fullName = categories.get(index).getFullName().toLowerCase();
        while (index > 0 && categories.get(index - 1).getFullName().toLowerCase().equals(fullName))
            index--;
        while (index < categories.size() && (categories.get(index).getFullName().toLowerCase().equals(fullName)
                || categories.get(index).getFullName().toLowerCase().startsWith(fullName + " / ")))
            result.add(categories.get(index++).getId());
        return result;
    }

    public Category getCategory(Category category, Long userId) {
        if (category.getId() != null && category.getId() > 0) {
            Category original = categoryRepository.findById(category.getId()).orElseThrow();
            if (original.getParentId() == null || original.getOwnerId().equals(userId)) {
                return original;
            }
        }
        var parent = getCategory(categoryRepository.findById(category.getParentId()).orElseThrow(), userId);
        var existing = categoryRepository
                .findAllByOwnerIdAndParentIdAndNameIgnoreCase(userId, parent.getId(), category.getName());
        if (existing.isEmpty()) {
            return categoryRepository.save(new Category(null, userId, parent.getId(), parent, category.getName(),
                    LocalDateTime.now(), LocalDateTime.now()));
        }
        return existing.get(0);
    }

    public Category saveCategory(Category category, Long userId) {
        Category original = getCategory(category, userId);
        original.setName(category.getName());
        original.setParentId(category.getParentId());
        original.setUpdated(LocalDateTime.now());
        categoryRepository.save(original);
        // remove dubs
        var dubs = categoryRepository.findAllByOwnerIdAndParentIdAndNameIgnoreCase(userId, original.getParentId(),
                original.getName());
        dubs.stream().filter(dub -> !dub.getId().equals(original.getId())).forEach(dub -> {
            transactionRepository.replaceCategoryId(dub.getId(), original.getId());
            ruleRepository.replaceCategoryId(dub.getId(), original.getId());
            categoryRepository.replaceParentId(dub.getId(), original.getId());
            categoryRepository.deleteById(dub.getId());
        });
        return original;
    }

    public void deleteCategory(Long id, Long replaceId, Long userId) {
        var category = categoryRepository.findById(id).orElseThrow();
        if (category.getOwnerId() == null || !category.getOwnerId().equals(userId)) {
            return;
        }
        if (replaceId == null) {
            replaceId = category.getParentId();
        }
        Category replace = getCategory(categoryRepository.findById(replaceId).orElseThrow(), userId);
        categoryRepository.replaceParentId(id, replaceId);
        replaceId = replace.getParentId() == null ? null : replace.getId();
        transactionRepository.replaceCategoryId(id, replaceId);
        if (replaceId == null) {
            ruleRepository.removeByCategoryId(id);
        } else {
            ruleRepository.replaceCategoryId(id, replaceId);
        }
        categoryRepository.delete(category);
    }
}

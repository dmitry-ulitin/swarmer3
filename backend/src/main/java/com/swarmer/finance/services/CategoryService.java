package com.swarmer.finance.services;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.swarmer.finance.dto.CategoryDto;
import com.swarmer.finance.exceptions.ResourceNotFoundException;
import com.swarmer.finance.models.Category;
import com.swarmer.finance.repositories.AclRepository;
import com.swarmer.finance.repositories.CategoryRepository;
import com.swarmer.finance.repositories.RuleRepository;
import com.swarmer.finance.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AclRepository aclRepository;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, AclRepository aclRepository,
            TransactionRepository transactionRepository, RuleRepository ruleRepository) {
        this.categoryRepository = categoryRepository;
        this.aclRepository = aclRepository;
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
    }

    // get all unic categories that the user has access to
    @Transactional
    public List<CategoryDto> getAllCategories(Long userId) {
        // get all user ids that the user has access to
        var ownerIds = LongStream.concat(LongStream.of(userId), aclRepository.findByUserIdOrderByGroupId(userId)
                .stream().mapToLong(acl -> acl.getGroup().getOwner().getId())).distinct().boxed().toList();
        var comparator = Comparator.comparing(CategoryDto::type)
                .thenComparingInt(c -> c.parentId() == null ? 0 : 1)
                .thenComparing((c1, c2) -> c1.fullName().compareToIgnoreCase(c2.fullName()))
                .thenComparingInt(c -> userId.equals(c.ownerId()) ? 0 : 1);
        // merge all categories with the same name
        return categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(ownerIds).stream()
                .map(CategoryDto::fromEntity)
                .sorted(comparator)
                .collect(Collectors.toMap(CategoryDto::fullName, c -> c, (c1, c2) -> c1))
                .values().stream()
                .sorted(comparator)
                .toList();
    }

    // get the category with specific userId or create it if it doesn't exist
    @Transactional
    public Category getCategory(CategoryDto dto, Long userId) {
        if (dto == null) {
            return null;
        }
        if (dto.id() != null && dto.id() > 0) {
            Category original = categoryRepository.findById(dto.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + dto.id()));
            // check if the category is root or belongs to the user
            if (original.getParent() == null || original.getOwnerId().equals(userId)) {
                return original;
            }
        }
        var parent = getCategory(
                CategoryDto.fromEntity(categoryRepository.findById(dto.parentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Parent category not found with id " + dto.parentId()))),
                userId);
        // check if the category with same name already exists
        var existing = categoryRepository.findAllByOwnerIdAndParentIdAndNameIgnoreCase(userId, parent.getId(),
                dto.name());
        if (existing.isEmpty()) {
            return categoryRepository.save(new Category(null, userId, parent, dto.name(),
                    LocalDateTime.now(), LocalDateTime.now()));
        }
        return existing.getFirst();
    }

    // save the category
    @Transactional
    public CategoryDto saveCategory(CategoryDto dto, Long userId) {
        Category category = getCategory(dto, userId);
        if (category.getId().equals(dto.id())) {
            category.setName(dto.name());
            category.setUpdated(LocalDateTime.now());
            categoryRepository.save(category);
        }
        // remove all duplicate categories
        categoryRepository.findAllByOwnerIdAndParentIdAndNameIgnoreCase(userId, category.getParent().getId(),
                category.getName()).stream()
                .filter(c -> !c.getId().equals(category.getId()))
                .forEach(d -> {
                    transactionRepository.replaceCategoryId(d.getId(), category.getId());
                    ruleRepository.replaceCategoryId(d.getId(), category.getId());
                    categoryRepository.deleteById(d.getId());
                });
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public void deleteCategory(Long id, Long userId) {
        Category original = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
        // check if the category belongs to the user and has a parent
        if (!original.getOwnerId().equals(userId) || original.getParent() == null) {
            throw new IllegalArgumentException();
        }
        // replace all references to this category with the parent
        categoryRepository.replaceParentId(id, original.getParent().getId());
        transactionRepository.replaceCategoryId(id, original.getParent().getId());
        if (original.getParent().getId() != null) {
            ruleRepository.replaceCategoryId(id, original.getParent().getId());
        } else {
            ruleRepository.removeByCategoryId(id);
        }
        ruleRepository.replaceCategoryId(id, original.getParent().getId());
        // delete the category
        categoryRepository.deleteById(id);
    }

    @Transactional
    public List<Long> getCategoriesFilter(Long userId, Long categoryId) {
        var ownerIds = LongStream.concat(LongStream.of(userId), aclRepository.findByUserIdOrderByGroupId(userId)
                .stream().mapToLong(acl -> acl.getGroup().getOwner().getId())).distinct().boxed().toList();
        var categories = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(ownerIds).stream()
                .map(CategoryDto::fromEntity).toList();
        var category = categories.stream().filter(c -> c.id().equals(categoryId)).findFirst().orElseThrow();
        return categories.stream()
                .filter(c -> c.type() == category.type() && (c.id().equals(categoryId)
                        || c.fullName().toLowerCase().startsWith(category.fullName().toLowerCase() + " / ")))
                .map(CategoryDto::id)
                .toList();
    }
}
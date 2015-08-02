package com.cspinformatique.kubik.domain.product.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.domain.product.repository.CategoryRepository;
import com.cspinformatique.kubik.domain.product.service.CategoryService;
import com.cspinformatique.kubik.domain.product.service.ProductService;
import com.cspinformatique.kubik.domain.product.service.SubCategoryService;
import com.cspinformatique.kubik.model.product.Category;
import com.cspinformatique.kubik.model.product.Product;
import com.cspinformatique.kubik.model.product.SubCategory;

@Service
public class CategoryServiceImpl implements CategoryService {
	@Autowired private CategoryRepository categoryRepository;
	
	@Autowired private ProductService productService;
	
	@Autowired private SubCategoryService subCategoryService;
	
	@Override
	public void delete(int id){
		Category category = this.findOne(id);
		for(Product product : productService.findByCategory(category)){
			product.setSubCategory(null);
			
			this.productService.save(product);
		}
		
		for(SubCategory subCategory : category.getSubCategories()){
			this.subCategoryService.delete(subCategory.getId());
		}
		
		categoryRepository.delete(id);
	}
	
	@Override
	public List<Category> findAll(){
		return categoryRepository.findAll();
	}
	
	@Override
	public Page<Category> findAll(Pageable pageable) {
		return categoryRepository.findAll(pageable);
	}

	@Override
	public Category findOne(int id) {
		return categoryRepository.findOne(id);
	}

	@Override
	public Category save(Category category) {
		return categoryRepository.save(category);
	}
}

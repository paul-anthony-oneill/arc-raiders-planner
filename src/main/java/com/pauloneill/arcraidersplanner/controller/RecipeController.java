package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.RecipeDto;
import com.pauloneill.arcraidersplanner.dto.RecipeIngredientDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.model.RecipeIngredient;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend dev server
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final ItemRepository itemRepository;

    public RecipeController(RecipeRepository recipeRepository, ItemRepository itemRepository) {
        this.recipeRepository = recipeRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all recipes")
    public List<RecipeDto> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create a new recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or recipe name already exists"),
            @ApiResponse(responseCode = "404", description = "Ingredient item not found")
    })
    public ResponseEntity<RecipeDto> createRecipe(@Valid @RequestBody RecipeDto recipeDto) {
        if (recipeRepository.findByName(recipeDto.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe with name " + recipeDto.name() + " already exists");
        }

        Recipe recipe = new Recipe();
        recipe.setName(recipeDto.name());
        recipe.setDescription(recipeDto.description());
        recipe.setType(recipeDto.type());

        for (RecipeIngredientDto ingredientDto : recipeDto.ingredients()) {
            Item item = itemRepository.findById(ingredientDto.itemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found with ID: " + ingredientDto.itemId()));
            
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setItem(item);
            ingredient.setQuantity(ingredientDto.quantity());
            recipe.addIngredient(ingredient);
        }

        Recipe savedRecipe = recipeRepository.save(recipe);
        return new ResponseEntity<>(convertToDto(savedRecipe), HttpStatus.CREATED);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recipe")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Recipe deleted"),
        @ApiResponse(responseCode = "404", description = "Recipe not found")
    })
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }
        recipeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private RecipeDto convertToDto(Recipe recipe) {
        List<RecipeIngredientDto> ingredients = recipe.getIngredients().stream()
                .map(ing -> new RecipeIngredientDto(
                        ing.getItem().getId(),
                        ing.getItem().getName(),
                        ing.getQuantity()))
                .collect(Collectors.toList());

        return new RecipeDto(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getType(),
                ingredients
        );
    }
}

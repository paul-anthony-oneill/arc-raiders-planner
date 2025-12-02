package com.pauloneill.arcraidersplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.RecipeDto;
import com.pauloneill.arcraidersplanner.dto.RecipeIngredientDto;
import com.pauloneill.arcraidersplanner.model.Item;
import com.pauloneill.arcraidersplanner.model.Recipe;
import com.pauloneill.arcraidersplanner.model.RecipeType;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeController.class)
public class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeRepository recipeRepository;

    @MockBean
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
    }

    @Test
    void testGetAllRecipes() throws Exception {
        when(recipeRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testCreateRecipe() throws Exception {
        RecipeIngredientDto ingredientDto = new RecipeIngredientDto(1L, "Test Item", 5);
        RecipeDto recipeDto = new RecipeDto(null, "metaforge_id_123", "New Recipe", "Description", RecipeType.CRAFTING, List.of(ingredientDto));

        when(recipeRepository.findByName("New Recipe")).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        
        Recipe savedRecipe = new Recipe();
        savedRecipe.setId(100L);
        savedRecipe.setName("New Recipe");
        savedRecipe.setDescription("Description");
        savedRecipe.setType(RecipeType.CRAFTING);
        // Mock ingredients not strictly necessary for this basic test unless we check response body deeply
        
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);

        mockMvc.perform(post("/api/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recipeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("New Recipe"));
    }
}

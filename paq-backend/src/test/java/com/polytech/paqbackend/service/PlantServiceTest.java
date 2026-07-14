package com.polytech.paqbackend.service;
import com.polytech.paqbackend.entity.Plant;
import com.polytech.paqbackend.entity.Site;
import com.polytech.paqbackend.repository.PlantRepository;
import com.polytech.paqbackend.repository.SiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private SegmentService segmentService;

    @InjectMocks
    private PlantService plantService;

    @Test
    void shouldGetPlantById(){

        Plant plant = new Plant();
        plant.setId(1L);

        when(plantRepository.findById(1L))
                .thenReturn(Optional.of(plant));

        Plant result = plantService.getById(1L);

        assertEquals(1L,result.getId());
    }

    @Test
    void shouldSavePlant(){

        Site site = new Site();
        site.setId(1L);

        Plant plant = new Plant();
        plant.setName("Plant A");
        plant.setSite(site);

        when(siteRepository.findById(1L))
                .thenReturn(Optional.of(site));

        when(plantRepository.save(any(Plant.class)))
                .thenReturn(plant);

        Plant saved = plantService.save(plant);

        assertEquals("Plant A",saved.getName());
    }

}

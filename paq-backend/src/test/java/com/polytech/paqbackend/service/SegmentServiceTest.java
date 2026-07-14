package com.polytech.paqbackend.service;


import com.polytech.paqbackend.dto.SegmentDTO;
import com.polytech.paqbackend.entity.Plant;
import com.polytech.paqbackend.entity.Segment;
import com.polytech.paqbackend.entity.Site;
import com.polytech.paqbackend.repository.PlantRepository;
import com.polytech.paqbackend.repository.SegmentRepository;
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
class SegmentServiceTest {

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private PlantRepository plantRepository;

    @InjectMocks
    private SegmentService segmentService;

    @Test
    void shouldCreateSegment(){

        Plant plant = new Plant();
        plant.setId(1L);

        SegmentDTO dto = new SegmentDTO();
        dto.setNomSegment("Segment A");
        dto.setPlantId(1L);

        when(plantRepository.findById(1L))
                .thenReturn(Optional.of(plant));

        when(segmentRepository.findByNomSegmentAndPlantId("Segment A",1L))
                .thenReturn(Optional.empty());

        Segment segment = Segment.builder()
                .nomSegment("Segment A")
                .plant(plant)
                .build();

        when(segmentRepository.save(any()))
                .thenReturn(segment);

        SegmentDTO result = segmentService.createSegment(dto);

        assertEquals("Segment A",result.getNomSegment());

    }

}

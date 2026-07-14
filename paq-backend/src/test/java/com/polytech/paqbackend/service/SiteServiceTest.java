package com.polytech.paqbackend.service;


import com.polytech.paqbackend.entity.Site;
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
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private SiteService siteService;

    @Test
    void shouldReturnAllSites() {

        Site s1 = new Site();
        s1.setId(1L);
        s1.setName("Site 1");

        Site s2 = new Site();
        s2.setId(2L);
        s2.setName("Site 2");

        when(siteRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Site> sites = siteService.getAll();

        assertEquals(2, sites.size());

        verify(siteRepository).findAll();
    }

    @Test
    void shouldReturnSiteById() {

        Site site = new Site();
        site.setId(1L);

        when(siteRepository.findById(1L))
                .thenReturn(Optional.of(site));

        Site result = siteService.getById(1L);

        assertEquals(1L, result.getId());

        verify(siteRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSiteNotFound() {

        when(siteRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> siteService.getById(1L));
    }

    @Test
    void shouldSaveSite() {

        Site site = new Site();
        site.setName("Site Test");

        when(siteRepository.save(site))
                .thenReturn(site);

        Site saved = siteService.save(site);

        assertEquals("Site Test", saved.getName());

        verify(siteRepository).save(site);
    }

    @Test
    void shouldDeleteSite() {

        siteService.delete(1L);

        verify(siteRepository).deleteById(1L);
    }

    @Test
    void shouldCountSites() {

        when(siteRepository.count()).thenReturn(5L);

        assertEquals(5, siteService.count());
    }

}

package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Book testBook;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");

        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateReservation_Success() {
        ReservationRequestDTO request = new ReservationRequestDTO(
                1L,
                258027L,
                7,
                LocalDate.now()
        );

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationResponseDTO result = reservationService.createReservation(request);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(258027L, result.getBookExternalId());

        verify(bookService, times(1)).decreaseAvailableQuantity(258027L);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void testCreateReservation_BookNotAvailable() {
        testBook.setAvailableQuantity(0);

        ReservationRequestDTO request = new ReservationRequestDTO(
                1L,
                258027L,
                7,
                LocalDate.now()
        );

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findByExternalId(258027L)).thenReturn(Optional.of(testBook));

        assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(request);
        });

        verify(reservationRepository, never()).save(any());
        verify(bookService, never()).decreaseAvailableQuantity(anyLong());
    }

    @Test
    void testReturnBook_OnTime() {
        ReturnBookRequestDTO request = new ReturnBookRequestDTO(
                testReservation.getExpectedReturnDate()
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getLateFee());

        verify(bookService, times(1))
                .increaseAvailableQuantity(testBook.getExternalId());
    }

    @Test
    void testReturnBook_Overdue() {
        ReturnBookRequestDTO request = new ReturnBookRequestDTO(
                testReservation.getExpectedReturnDate().plusDays(3)
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.OVERDUE, result.getStatus());
        assertTrue(result.getLateFee().compareTo(BigDecimal.ZERO) > 0);

        verify(bookService, times(1))
                .increaseAvailableQuantity(testBook.getExternalId());
    }

    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationResponseDTO result = reservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }

    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();

        // FIX: agregamos user y book
        reservation2.setId(2L);
        reservation2.setUser(testUser);
        reservation2.setBook(testBook);
        reservation2.setCreatedAt(LocalDateTime.now());
        reservation2.setStartDate(LocalDate.now());
        reservation2.setExpectedReturnDate(LocalDate.now());

        when(reservationRepository.findAll())
                .thenReturn(Arrays.asList(testReservation, reservation2));

        List<ReservationResponseDTO> result = reservationService.getAllReservations();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getActiveReservations();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

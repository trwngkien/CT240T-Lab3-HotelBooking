package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Test Suite - Bài tập 1")
class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository; // Giả lập CSDL phòng

    @Mock
    private PaymentGateway paymentGateway; // Giả lập cổng thanh toán[cite: 1]

    @InjectMocks
    private BookingService bookingService; // Tiêm các mock vào BookingService[cite: 1]

    @Test
    @DisplayName("Luồng thành công: Đặt phòng thành công, chuyển trạng thái và gọi mock đúng số lần")
    void testBookRoom_Success() {
        // ARRANGE
        Room room = new Room("R101", 500000, true);
        when(roomRepository.findById("R101")).thenReturn(Optional.of(room));
        when(paymentGateway.charge(anyString(), anyDouble())).thenReturn(true);

        // ACT
        BookingReceipt receipt = bookingService.bookRoom("R101", 2, "123456789");

        // ASSERT
        assertNotNull(receipt);
        assertEquals("R101", receipt.getRoomId());
        assertEquals(1000000.0, receipt.getTotalAmount());
        assertFalse(room.isAvailable()); // Trạng thái phòng chuyển sang false[cite: 1]

        // Verification số lần gọi hàm[cite: 1]
        verify(roomRepository, times(1)).findById("R101");
        verify(paymentGateway, times(1)).charge("123456789", 1000000.0);
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy phòng (Room Not Found)")
    void testBookRoom_RoomNotFound_ThrowsException() {
        // ARRANGE: findById trả về Optional.empty()[cite: 1]
        when(roomRepository.findById("R999")).thenReturn(Optional.empty());

        // ACT & ASSERT
        Exception exception = assertThrows(RoomNotAvailableException.class, () -> {
            bookingService.bookRoom("R999", 1, "123456789");
        });
        assertEquals("Khong tim thay phong: R999", exception.getMessage());
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi phòng đã được đặt trước đó (Already Booked)")
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        // ARRANGE: room.isAvailable() == false[cite: 1]
        Room room = new Room("R101", 500000, false);
        when(roomRepository.findById("R101")).thenReturn(Optional.of(room));

        // ACT & ASSERT
        Exception exception = assertThrows(RoomNotAvailableException.class, () -> {
            bookingService.bookRoom("R101", 1, "123456789");
        });
        assertEquals("Phong da duoc dat truoc do: R101", exception.getMessage());
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi thanh toán thất bại (Payment Failed)")
    void testBookRoom_PaymentFailed_ThrowsException() {
        // ARRANGE: charge trả về false[cite: 1]
        Room room = new Room("R101", 500000, true);
        when(roomRepository.findById("R101")).thenReturn(Optional.of(room));
        when(paymentGateway.charge(anyString(), anyDouble())).thenReturn(false);

        // ACT & ASSERT
        Exception exception = assertThrows(PaymentFailedException.class, () -> {
            bookingService.bookRoom("R101", 1, "123456789");
        });
        assertEquals("Thanh toan qua the credit card that bai!", exception.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kiểm thử tham số biên: Số đêm không hợp lệ (nights <= 0)")
    void testBookRoom_InvalidNights_ThrowsException() {
        // ACT & ASSERT: Kiểm thử với nights = 0 hoặc -1[cite: 1]
        Exception ex1 = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 0, "123456789");
        });
        Exception ex2 = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", -1, "123456789");
        });

        assertEquals("Tham so dat phong khong hop le!", ex1.getMessage());
        assertEquals("Tham so dat phong khong hop le!", ex2.getMessage());
        verifyNoInteractions(roomRepository, paymentGateway);
    }

    @Test
    @DisplayName("Kiểm thử tham số biên: Thẻ tín dụng null hoặc rỗng")
    void testBookRoom_NullOrEmptyCreditCard_ThrowsException() {
        // ACT & ASSERT: creditCard == null hoặc ""[cite: 1]
        Exception ex1 = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 1, null);
        });
        Exception ex2 = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 1, "   ");
        });

        assertEquals("Tham so dat phong khong hop le!", ex1.getMessage());
        assertEquals("Tham so dat phong khong hop le!", ex2.getMessage());
        verifyNoInteractions(roomRepository, paymentGateway);
    }
}
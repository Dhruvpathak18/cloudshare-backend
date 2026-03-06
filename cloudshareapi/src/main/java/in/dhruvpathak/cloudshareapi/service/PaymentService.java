package in.dhruvpathak.cloudshareapi.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import in.dhruvpathak.cloudshareapi.document.PaymentTransaction;
import in.dhruvpathak.cloudshareapi.document.ProfileDocument;
import in.dhruvpathak.cloudshareapi.dto.PaymentDTO;
import in.dhruvpathak.cloudshareapi.dto.PaymentVerificationDTO;
import in.dhruvpathak.cloudshareapi.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Formatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ---------------- CREATE ORDER ----------------
    public PaymentDTO createOrder(PaymentDTO paymentDTO) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            RazorpayClient razorpayClient =
                    new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paymentDTO.getAmount());
            orderRequest.put("currency", paymentDTO.getCurrency());
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            // ✅ SAVE PAYMENT TRANSACTION
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(paymentDTO.getAmount())
                    .currency(paymentDTO.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order created successfully")
                    .build();

        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order: " + e.getMessage())
                    .build();
        }
    }

    // ---------------- VERIFY PAYMENT ----------------
    public PaymentDTO verifyPayment(PaymentVerificationDTO request) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();

            String data = request.getRazorpay_order_id()
                    + "|" + request.getRazorpay_payment_id();

            String generatedSignature =
                    generateHmacSha256Signature(data, razorpayKeySecret);

            if (!generatedSignature.equals(request.getRazorpay_signature())) {
                updateTransactionStatus(
                        clerkId,
                        request.getRazorpay_order_id(),
                        "FAILED",
                        request.getRazorpay_payment_id(),
                        null
                );
                return PaymentDTO.builder()
                        .success(false)
                        .message("Payment signature verification failed")
                        .build();
            }

            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (request.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    plan = "PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }

            if (creditsToAdd > 0) {
                userCreditsService.addCredits(clerkId, creditsToAdd, plan);

                updateTransactionStatus(
                        clerkId,
                        request.getRazorpay_order_id(),
                        "SUCCESS",
                        request.getRazorpay_payment_id(),
                        creditsToAdd
                );

                return PaymentDTO.builder()
                        .success(true)
                        .message("Payment verified and credits added successfully")
                        .credits(
                                userCreditsService
                                        .getUserCredits(clerkId)
                                        .getCredits()
                        )
                        .build();
            }

            updateTransactionStatus(
                    clerkId,
                    request.getRazorpay_order_id(),
                    "FAILED",
                    request.getRazorpay_payment_id(),
                    null
            );

            return PaymentDTO.builder()
                    .success(false)
                    .message("Invalid plan selected")
                    .build();

        } catch (Exception e) {
            updateTransactionStatus(
                    profileService.getCurrentProfile().getClerkId(),
                    request.getRazorpay_order_id(),
                    "ERROR",
                    request.getRazorpay_payment_id(),
                    null
            );

            return PaymentDTO.builder()
                    .success(false)
                    .message("Error verifying payment: " + e.getMessage())
                    .build();
        }
    }

    // ---------------- UPDATE TRANSACTION ----------------
    private void updateTransactionStatus(
            String clerkId,
            String orderId,
            String status,
            String paymentId,
            Integer creditsAdded
    ) {
        paymentTransactionRepository
                .findByClerkId(clerkId)
                .stream()
                .filter(tx -> orderId.equals(tx.getOrderId()))
                .findFirst()
                .ifPresent(tx -> {
                    tx.setStatus(status);
                    tx.setPaymentId(paymentId);
                    if (creditsAdded != null) {
                        tx.setCreditsAdded(creditsAdded);
                    }
                    paymentTransactionRepository.save(tx);
                });
    }

    // ---------------- SIGNATURE UTILS ----------------
    private String generateHmacSha256Signature(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec secretKey =
                new SecretKeySpec(secret.getBytes(), "HmacSHA256");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        byte[] hmacData = mac.doFinal(data.getBytes());
        return toHexString(hmacData);
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}

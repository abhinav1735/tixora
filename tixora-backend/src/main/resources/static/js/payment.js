
const paymentForm = document.getElementById("paymentForm");
const payButton = document.getElementById("payButton");
const paymentMessage = document.getElementById("paymentMessage");

paymentForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const bookingReference = document
        .getElementById("bookingReference")
        .value.trim();

    if (!bookingReference) {
        showMessage("Please enter your booking reference.");
        return;
    }

    payButton.disabled = true;
    payButton.textContent = "Creating payment order...";

    try {
        // Step 1: Create Razorpay order through our backend.
        const orderResponse = await fetch("/api/payments/create-order", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ bookingReference })
        });

        const orderData = await orderResponse.json();

        if (!orderResponse.ok) {
            throw new Error(
                orderData.message || "Unable to create payment order."
            );
        }

        // Step 2: Configure Razorpay Checkout.
        const options = {
            key: orderData.razorpayKeyId,
            amount: Math.round(Number(orderData.amount) * 100),
            currency: orderData.currency,
            name: "Tixora",
            description: "Movie Ticket Booking",
            order_id: orderData.razorpayOrderId,

            prefill: {
                name: orderData.customerName,
                email: orderData.customerEmail
            },

            theme: {
                color: "#6C63FF"
            },

            // Step 3: Verify successful payment on the backend.
            handler: async function (response) {
                showMessage("Verifying your payment...");

                try {
                    const verifyResponse = await fetch(
                        "/api/payments/verify",
                        {
                            method: "POST",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify({
                                razorpayOrderId: response.razorpay_order_id,
                                razorpayPaymentId: response.razorpay_payment_id,
                                razorpaySignature: response.razorpay_signature
                            })
                        }
                    );

                    const verifyData = await verifyResponse.json();

                    if (!verifyResponse.ok) {
                        throw new Error(
                            verifyData.message ||
                            "Payment verification failed."
                        );
                    }

                    if (verifyData.bookingStatus === "CONFIRMED") {
                        showMessage(
                            "Payment successful! Your booking is confirmed. Reference: " +
                            verifyData.bookingReference
                        );
                    } else {
                        showMessage(
                            verifyData.message ||
                            "Payment received, but booking confirmation needs attention."
                        );
                    }

                } catch (error) {
                    showMessage(error.message);
                } finally {
                    payButton.disabled = false;
                    payButton.textContent = "Continue to Payment";
                }
            },

            // Step 4: Handle checkout modal dismissal.
            modal: {
                ondismiss: function () {
                    showMessage(
                        "Payment window closed. Your booking may still be pending."
                    );

                    payButton.disabled = false;
                    payButton.textContent = "Continue to Payment";
                }
            }
        };

        const razorpay = new Razorpay(options);

        // Step 5: Handle failed payment attempts.
        razorpay.on("payment.failed", async function (response) {
            const metadata = response.error?.metadata;

            const razorpayOrderId = metadata?.order_id;
            const razorpayPaymentId = metadata?.payment_id;

            if (!razorpayOrderId || !razorpayPaymentId) {
                showMessage(
                    response.error?.description ||
                    "Payment failed. Unable to identify the payment attempt."
                );

                payButton.disabled = false;
                payButton.textContent = "Continue to Payment";
                return;
            }

            showMessage("Payment failed. Recording the failed attempt...");

            try {
                const failureResponse = await fetch(
                    "/api/payments/failure",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            razorpayOrderId: razorpayOrderId,
                            razorpayPaymentId: razorpayPaymentId
                        })
                    }
                );

                const failureData = await failureResponse.json();

                if (!failureResponse.ok) {
                    throw new Error(
                        failureData.message ||
                        "Unable to record failed payment."
                    );
                }

                showMessage(
                    failureData.message ||
                    "Payment failed. You can try again."
                );

            } catch (error) {
                showMessage(
                    "Payment failed. " + error.message
                );
            } finally {
                payButton.disabled = false;
                payButton.textContent = "Continue to Payment";
            }
        });

        // Step 6: Open Razorpay Checkout.
        razorpay.open();

    } catch (error) {
        showMessage(error.message);

        payButton.disabled = false;
        payButton.textContent = "Continue to Payment";
    }
});

function showMessage(message) {
    paymentMessage.textContent = message;
}

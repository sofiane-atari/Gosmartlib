package edu.ap.bib_backend.service.message;

public class DefaultMessageStyles implements MessageStyles {

    @Override
    public String getStyleBlock() {
        return """
                <style>
                    .msg-btn-primary {
                        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
                    }
                    .msg-btn-primary:hover {
                        background-color: #6b1532 !important;
                        transform: translateY(-1px) !important;
                        box-shadow: 0 4px 14px rgba(138, 30, 65, 0.35) !important;
                    }

                    .msg-book-link {
                        transition: color 0.2s ease !important;
                    }
                    .msg-book-link:hover {
                        color: #8a1e41 !important;
                    }

                    .msg-book-card {
                        transition: all 0.2s ease !important;
                    }
                    .msg-book-card:hover {
                        background-color: #fdf0f5 !important;
                        border-color: #d4b0bc !important;
                    }

                    .msg-cover-link {
                        transition: opacity 0.2s ease !important;
                    }
                    .msg-cover-link:hover {
                        opacity: 0.85 !important;
                    }
                </style>
                """;
    }
}

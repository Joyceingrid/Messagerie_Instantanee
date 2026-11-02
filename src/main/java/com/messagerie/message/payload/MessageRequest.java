package com.messagerie.message.payload;

public record MessageRequest(String type, Long destinataireId, String contenu) {}

package com.tamo.model;

/** An email request committed with the published placement and consumed by the IT mail worker. */
public record OfferEmail(String id, String recipientEmail, String courseCode, String courseName,
                         String sectionName, String placementPath, String createdAt) {}

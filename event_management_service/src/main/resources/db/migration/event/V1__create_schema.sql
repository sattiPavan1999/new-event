-- Create events schema
CREATE SCHEMA IF NOT EXISTS events;

-- Create venues table
CREATE TABLE events.venues (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    capacity INTEGER NOT NULL,
    CONSTRAINT venues_capacity_positive CHECK (capacity > 0)
);

-- Create events table
CREATE TABLE events.events (
    id UUID PRIMARY KEY,
    organiser_id UUID NOT NULL,
    venue_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    banner_image_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT events_venue_fk FOREIGN KEY (venue_id) REFERENCES events.venues(id),
    CONSTRAINT events_category_check CHECK (category IN ('CONCERT', 'SPORTS', 'CONFERENCE', 'OTHER')),
    CONSTRAINT events_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

-- Create ticket_tiers table
CREATE TABLE events.ticket_tiers (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    total_qty INTEGER NOT NULL,
    remaining_qty INTEGER NOT NULL,
    max_per_order INTEGER NOT NULL DEFAULT 10,
    sale_starts_at TIMESTAMP,
    sale_ends_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ticket_tiers_event_fk FOREIGN KEY (event_id) REFERENCES events.events(id) ON DELETE CASCADE,
    CONSTRAINT ticket_tiers_price_positive CHECK (price >= 0),
    CONSTRAINT ticket_tiers_total_qty_positive CHECK (total_qty > 0),
    CONSTRAINT ticket_tiers_remaining_qty_check CHECK (remaining_qty >= 0),
    CONSTRAINT ticket_tiers_max_per_order_positive CHECK (max_per_order > 0),
    CONSTRAINT ticket_tiers_status_check CHECK (status IN ('ACTIVE', 'CLOSED', 'SOLD_OUT'))
);

-- Create indexes for performance
CREATE INDEX idx_events_status ON events.events(status);
CREATE INDEX idx_events_event_date ON events.events(event_date);
CREATE INDEX idx_events_category ON events.events(category);
CREATE INDEX idx_events_organiser_id ON events.events(organiser_id);
CREATE INDEX idx_events_venue_id ON events.events(venue_id);
CREATE INDEX idx_ticket_tiers_event_id ON events.ticket_tiers(event_id);
CREATE INDEX idx_venues_city ON events.venues(city);

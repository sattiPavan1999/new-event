-- Composite index for the public browse-events query (status + event_date + category)
CREATE INDEX idx_events_published_browse ON events.events(status, event_date, category);

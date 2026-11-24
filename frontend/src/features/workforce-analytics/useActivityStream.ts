import { useEffect, useState, useRef, useCallback } from 'react';
import { useAuthStore } from '../../store/authStore';
import { API_BASE_URL } from '../../api/client';
import { ActivityTimelineItem } from './types';

export interface UseActivityStreamReturn {
  isConnected: boolean;
  lastEvent: ActivityTimelineItem | null;
  events: ActivityTimelineItem[];
  reconnect: () => void;
}

export const useActivityStream = (targetUserId?: string): UseActivityStreamReturn => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [lastEvent, setLastEvent] = useState<ActivityTimelineItem | null>(null);
  const [events, setEvents] = useState<ActivityTimelineItem[]>([]);
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<any>(null);

  const connect = useCallback(() => {
    const token = useAuthStore.getState().accessToken;
    if (!token) return;

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    // Determine stream endpoint (works in local dev and cloud production)
    const streamUrl = `${API_BASE_URL}/v1/stream/activity?token=${encodeURIComponent(token)}`;
    const es = new EventSource(streamUrl);
    eventSourceRef.current = es;

    es.onopen = () => {
      setIsConnected(true);
    };

    es.addEventListener('connected', () => {
      setIsConnected(true);
    });

    es.addEventListener('activity', (event: MessageEvent) => {
      try {
        const raw = JSON.parse(event.data);
        const item: ActivityTimelineItem = {
          id: raw.id || String(Date.now()),
          eventType: raw.eventType || 'ACTIVITY',
          title: raw.title || 'WorkHive Activity Event',
          description: raw.description || 'Live telemetry heartbeat received',
          timestamp: raw.createdAt || new Date().toISOString(),
          metadataJson: raw.metadataJson,
        };

        if (raw.metadataJson) {
          try {
            const parsedMeta = JSON.parse(raw.metadataJson);
            item.repository = parsedMeta.repository;
            item.branch = parsedMeta.branch;
            item.commitSha = parsedMeta.commitSha;
            item.externalUrl = parsedMeta.externalUrl;
          } catch (ignored) {}
        }

        // If targetUserId specified, only filter if matching (or show all if global)
        if (!targetUserId || !raw.userId || raw.userId === targetUserId) {
          setLastEvent(item);
          setEvents((prev) => [item, ...prev.slice(0, 49)]);
        }
      } catch (err) {
        console.warn('Failed to parse SSE activity payload', err);
      }
    });

    es.onerror = () => {
      setIsConnected(false);
      es.close();
      // Auto-reconnect after 6 seconds
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = setTimeout(() => {
        connect();
      }, 6000);
    };
  }, [targetUserId]);

  useEffect(() => {
    connect();
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [connect]);

  return {
    isConnected,
    lastEvent,
    events,
    reconnect: connect,
  };
};

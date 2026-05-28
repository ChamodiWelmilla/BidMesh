import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketService {
    private client: Client;

    constructor(onMessage: (message: any) => void, auctionId: number) {
        this.client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:9000/ws-auction'),
            onConnect: () => {
                console.log('Connected to WebSocket');
                this.client.subscribe(`/topic/auction/${auctionId}`, (message) => {
                    onMessage(JSON.parse(message.body));
                });
            },
            onStompError: (frame) => {
                console.error('STOMP Error:', frame);
            }
        });
    }

    activate() {
        this.client.activate();
    }

    deactivate() {
        this.client.deactivate();
    }
}

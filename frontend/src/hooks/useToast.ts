import { useState, useCallback } from 'react';
import type { ToastType } from '../components/Toast';

interface ToastNotification {
    message: string;
    type: ToastType;
    id: number;
}

let toastIdCounter = 0;

export const useToast = () => {
    const [toasts, setToasts] = useState<ToastNotification[]>([]);

    const showToast = useCallback((message: string, type: ToastType = 'info') => {
        const id = Date.now() + (toastIdCounter++);
        setToasts(prev => [...prev, { message, type, id }]);
    }, []);

    const hideToast = useCallback((id: number) => {
        setToasts(prev => prev.filter(toast => toast.id !== id));
    }, []);

    return { toasts, showToast, hideToast };
};

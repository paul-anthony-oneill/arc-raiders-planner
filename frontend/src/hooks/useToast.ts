import { useState, useCallback } from 'react';
import type { ToastType } from '../components/Toast';

interface ToastNotification {
    message: string;
    type: ToastType;
    id: string;
}



export const useToast = () => {
    const [toasts, setToasts] = useState<ToastNotification[]>([]);

    const showToast = useCallback((message: string, type: ToastType = 'info') => {
        const id = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substr(2, 9);
        setToasts(prev => [...prev, { message, type, id }]);
    }, []);

    const hideToast = useCallback((id: string) => {
        setToasts(prev => prev.filter(toast => toast.id !== id));
    }, []);

    return { toasts, showToast, hideToast };
};

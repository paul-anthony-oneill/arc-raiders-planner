import React, { useEffect } from 'react';

export type ToastType = 'error' | 'success' | 'info';

interface ToastProps {
    message: string;
    type: ToastType;
    onClose: () => void;
    duration?: number;
}

export const Toast: React.FC<ToastProps> = ({ message, type, onClose, duration = 5000 }) => {
    useEffect(() => {
        const timer = setTimeout(onClose, duration);
        return () => clearTimeout(timer);
    }, [duration, onClose]);

    const styles = {
        error: 'border-retro-red text-retro-red bg-retro-red/10',
        success: 'border-retro-green text-retro-green bg-retro-green/10',
        info: 'border-retro-orange text-retro-orange bg-retro-orange/10'
    };

    const titles = {
        error: 'SYSTEM ERROR',
        success: 'SUCCESS',
        info: 'NOTICE'
    };

    return (
        <div
            className={`fixed top-20 right-4 z-50 px-6 py-4 border-l-4 shadow-lg animate-slide-in max-w-md ${styles[type]}`}
            role="alert"
            aria-live="assertive"
        >
            <h4 className="font-bold uppercase tracking-wider text-xs mb-1 font-mono">
                {titles[type]}
            </h4>
            <p className="text-sm text-retro-sand font-mono">{message}</p>
            <button
                onClick={onClose}
                className="absolute top-2 right-2 text-retro-sand/50 hover:text-retro-sand"
                aria-label="Close notification"
            >
                ✕
            </button>
        </div>
    );
};

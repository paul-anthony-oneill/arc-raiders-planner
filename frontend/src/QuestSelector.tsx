import React from 'react';
import type { Quest } from './types';

interface QuestSelectorProps {
    quests: Quest[];
    selectedQuestIds: string[];
    onSelectQuest: (questId: string) => void;
    onDeselectQuest: (questId: string) => void;
}

const QuestSelector: React.FC<QuestSelectorProps> = ({ quests, selectedQuestIds, onSelectQuest, onDeselectQuest }) => {
    const handleQuestChange = (questId: string, checked: boolean) => {
        if (checked) {
            onSelectQuest(questId);
        } else {
            onDeselectQuest(questId);
        }
    };

    return (
        <div className="bg-retro-dark p-4 h-full overflow-y-auto">
            <h2 className="text-lg font-bold text-retro-sand mb-4">Quests</h2>
            <ul>
                {quests.map((quest) => (
                    <li key={quest.id} className="flex items-center mb-2">
                        <input
                            type="checkbox"
                            id={`quest-${quest.id}`}
                            checked={selectedQuestIds.includes(quest.id)}
                            onChange={(e) => handleQuestChange(quest.id, e.target.checked)}
                            className="mr-2"
                        />
                        <label htmlFor={`quest-${quest.id}`} className="text-retro-sand">
                            {quest.name}
                        </label>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default QuestSelector;

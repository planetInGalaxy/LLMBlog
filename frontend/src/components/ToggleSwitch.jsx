import React from 'react';

export default function ToggleSwitch({ checked, onChange, label, hint, disabled }) {
  return (
    <div className="toggle-row">
      <label className={`toggle-switch${disabled ? ' is-disabled' : ''}`}>
        <input
          type="checkbox"
          checked={!!checked}
          onChange={onChange}
          disabled={!!disabled}
        />
        <span className="slider" aria-hidden="true" />
        <span className="toggle-label">
          <span className="toggle-title">{label}</span>
          {hint ? <span className="toggle-hint">{hint}</span> : null}
        </span>
      </label>
    </div>
  );
}

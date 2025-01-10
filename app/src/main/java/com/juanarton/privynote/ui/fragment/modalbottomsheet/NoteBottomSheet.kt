package com.juanarton.privynote.ui.fragment.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juanarton.privynote.databinding.FragmentNoteBottomSheetBinding

class NoteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentNoteBottomSheetBinding? = null
    private val binding get() = _binding

    private var callback: NoteBottomSheetCallback? = null

    companion object {
        const val NOTEBOTTOMSHEET = "NoteBottomSheet"
        const val DELETE = 0
    }

    fun onMenuSelected(callback: NoteBottomSheetCallback) {
        this.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNoteBottomSheetBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            deleteClickmask.setOnClickListener {
                callback?.onMenuSelected(DELETE)
                dismiss()
            }
        }
    }
}
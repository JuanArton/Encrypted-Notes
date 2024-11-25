package com.juanarton.encnotes.ui.fragment.insertkey

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.juanarton.encnotes.R
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.databinding.FragmentInsertKeyBinding
import com.juanarton.encnotes.ui.LoadingDialog
import com.juanarton.encnotes.ui.activity.main.MainActivity
import com.juanarton.encnotes.ui.fragment.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InsertKeyFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private var _binding: FragmentInsertKeyBinding? = null
    private val binding get() = _binding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var key: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInsertKeyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingDialog(requireContext())

        val uid = arguments?.getString("uid")
        val pin = arguments?.getString("pin")

        if (!uid.isNullOrEmpty() && !pin.isNullOrEmpty()) {
            binding?.apply {
                btDone.setOnClickListener {
                    key = etCipherKey.text.toString()
                    if (key.isNotEmpty()) {
                        sharedViewModel.loginUser(uid, pin)
                    } else {
                        Toast.makeText(
                            requireContext(), getString(R.string.please_insert_cipher_key
                        ), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            sharedViewModel.loginUser.observe(viewLifecycleOwner) { result ->
                when(result){
                    is Resource.Success -> {
                        result.data?.let { login ->
                            lifecycleScope.launch {
                                val setAccKey = sharedViewModel.setAccessKey(login.accessToken)
                                val setRefKey = sharedViewModel.setRefreshKey(login.refreshToken)
                                val setLoggedIn = sharedViewModel.setIsLoggedIn(true)
                                val setCipherKey = if (::key.isInitialized){
                                    sharedViewModel.setCipherKey(key)
                                } else { false }

                                if (setAccKey && setRefKey && setLoggedIn && setCipherKey) {
                                    loadingDialog.dismiss()
                                    startActivity(Intent(requireContext(), MainActivity::class.java))
                                    requireActivity().finish()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadingDialog.dismiss()
                                }
                            }
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("Copy Key Fragment", "Loading")
                        loadingDialog.show()
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            result.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingDialog.dismiss()
                    }
                }
            }
        }
    }
}